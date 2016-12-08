package uk.gov.hmrc.eeitt.controllers

import play.Logger
import play.api.libs.json.{ Format, JsError, JsSuccess, Json, Reads, Writes }
import play.api.mvc._
import uk.gov.hmrc.eeitt.model._
import uk.gov.hmrc.eeitt.services._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.eeitt.repositories._

import scala.concurrent.Future

object RegistrationController extends RegistrationController {

  import uk.gov.hmrc.eeitt.implicits._

  def verification(groupId: GroupId, regimeId: RegimeId, affinityGroup: AffinityGroup) = affinityGroup match {
    case Agent => verify(groupId)
    case _ => verify((groupId, regimeId))
  }

  def prepopulationAgent(groupId: GroupId) = prepopulate[GroupId, RegistrationAgent](groupId)

  def prepopulationBusinessUser(groupId: GroupId, regimeId: RegimeId) = prepopulate[(GroupId, RegimeId), RegistrationBusinessUser]((groupId, regimeId))

  def registerBusinessUser = register[RegisterBusinessUserRequest, EtmpBusinessUser]

  def registerAgent = register[RegisterAgentRequest, EtmpAgent]

}

trait RegistrationController extends BaseController {

  def verify[A](findParams: A)(
    implicit
    findRegistration: FindRegistration[A]
  ) = Action.async { implicit request =>
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

  def register[A <: RegisterRequest: Reads, B: PostcodeValidator](
    implicit
    findRegistration: FindRegistration[A],
    findUser: FindUser[A, B],
    addReg: A => Future[Either[String, Unit]]
  ) = Action.async(parse.json) { implicit request =>
    request.body.validate match {
      case JsSuccess(req, _) =>
        RegistrationService.register(req).map(response => Ok(Json.toJson(response)))
      case JsError(jsonErrors) =>
        Logger.debug(s"incorrect request: $jsonErrors ")
        Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(jsonErrors))))
    }
  }
}
