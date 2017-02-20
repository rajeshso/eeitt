package uk.gov.hmrc.eeitt.controllers

import play.api.Logger
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.eeitt.model._
import uk.gov.hmrc.eeitt.services._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.eeitt.repositories._
import uk.gov.hmrc.eeitt.typeclasses.{ HmrcAudit, SendRegistered }

import scala.concurrent.Future

class RegistrationController(
    val messagesApi: MessagesApi
)(
    implicit
    registrationRepository: MongoRegistrationBusinessUserRepository,
    agentRegistrationRepository: MongoRegistrationAgentRepository,
    etmpBusinessUserRepository: MongoEtmpBusinessUsersRepository,
    etmpAgentRepository: MongoEtmpAgentRepository,
    auditService: AuditService
) extends RegistrationControllerHelper {

  def verification(groupId: GroupId, regimeId: RegimeId, affinityGroup: AffinityGroup) = affinityGroup match {
    case Agent =>
      Logger.info(s"verification - agent - groupId '${groupId.value}'")
      verify(groupId)
    case _ =>
      Logger.info(s"verification - business user - groupId '${groupId.value}', regimeId '${regimeId.value}'")
      verify((groupId, regimeId))
  }

  def prepopulationAgent(groupId: GroupId) = {
    Logger.info(s"prepopulation - agent - groupId '${groupId.value}'")
    prepopulate[GroupId, RegistrationAgent](groupId)
  }

  def prepopulationBusinessUser(groupId: GroupId, regimeId: RegimeId) = {
    Logger.info(s"prepopulation - business user - groupId '${groupId.value}', regimeId '${regimeId.value}'")
    prepopulate[(GroupId, RegimeId), RegistrationBusinessUser]((groupId, regimeId))
  }

  def registerBusinessUser = register[RegisterBusinessUserRequest, EtmpBusinessUser]

  def registerAgent = register[RegisterAgentRequest, EtmpAgent]
}

trait RegistrationControllerHelper extends BaseController with I18nSupport {

  def verify[A: FindRegistration](findParams: A) = Action.async { implicit request =>
    RegistrationService.findRegistration(findParams)
      .map(_.size == 1)
      .map(response => Ok(Json.toJson(VerificationResponse(response))))
  }

  def prepopulate[A, B](findParams: A)(
    implicit
    findRegistration: FindRegistration.Aux[A, B],
    prepopulation: PrepopulationData[B],
    show: Show[A]
  ) = Action.async { implicit request =>
    RegistrationService.findRegistration(findParams).map {
      case registration :: Nil =>
        Ok(prepopulation(registration))
      case Nil =>
        Logger.warn(s"Prepopulation data not found for ${show(findParams)}")
        NotFound
      case registration :: xs =>
        Logger.warn(s"More than one prepopulation data found for ${show(findParams)}")
        Ok(prepopulation(registration))
    }
  }

  def register[A <: RegisterRequest: Reads: AddRegistration: FindRegistration, B: PostcodeValidator](
    implicit
    findUser: FindUser[A, B],
    sendRegistered: SendRegistered[A],
    hmrcAudit: HmrcAudit[AuditData]
  ): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val messages = messagesApi.preferred(request)
    request.body.validate match {
      case JsSuccess(req, _) =>
        RegistrationService.register(req).map { regRes =>
          regRes match {
            case RESPONSE_OK =>
              val (postCode, tags) = sendRegistered(req)
              val ad = new AuditData(request.path, postCode, tags)
              hmrcAudit(ad)(hc(request)) // send audit data
            case _ =>
          }
          Ok(regRes.toJson(messages))
        }
      case JsError(jsonErrors) =>
        Logger.debug(s"incorrect request: $jsonErrors ")

        val response = jsonErrors match {
          // This occurs when registrationNumber is less that 15 characters. We want in such a case
          // return proper response (200) to the client.
          case (JsPath(KeyPathNode("registrationNumber") :: _), _) :: _ =>
            findUser match {
              case agent: FindUser[RegisterAgentRequest, PostcodeValidator[EtmpAgent]] =>
                Ok(INCORRECT_KNOWN_FACTS_AGENTS.toJson(messages))
              case _ =>
                Ok(INCORRECT_KNOWN_FACTS_BUSINESS_USERS.toJson(messages))
            }
          case _ => BadRequest(Json.obj("message" -> JsError.toJson(jsonErrors)))
        }
        Future.successful(response)
    }
  }
}
