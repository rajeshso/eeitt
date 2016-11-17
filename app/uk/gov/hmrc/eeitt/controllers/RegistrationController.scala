package uk.gov.hmrc.eeitt.controllers

import play.Logger
import play.api.libs.json.{ Format, JsError, JsSuccess, Json }
import play.api.mvc._
import uk.gov.hmrc.eeitt.model.{ AffinityGroup, Agent, AgentRegistration, IndividualRegistration, RegisterRequest }
import uk.gov.hmrc.eeitt.model.RegisterAgentRequest
import uk.gov.hmrc.eeitt.services.{ RegistrationService, UserExists }
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.eeitt.model.{ GroupId, RegimeId }
import uk.gov.hmrc.eeitt.services.Verification
import uk.gov.hmrc.eeitt.services.VerificationRepo

import scala.concurrent.Future

object RegistrationController extends RegistrationController {
  val registrationService = RegistrationService
}

trait RegistrationController extends BaseController {
  val registrationService: RegistrationService

  implicit lazy val registrationRepository = uk.gov.hmrc.eeitt.repositories.registrationRepository
  implicit lazy val agentRegistrationRepository = uk.gov.hmrc.eeitt.repositories.agentRegistrationRepository
  implicit lazy val etmpBusinessUserRepository = uk.gov.hmrc.eeitt.repositories.etmpBusinessUserRepository
  implicit lazy val etmpAgentRepository = uk.gov.hmrc.eeitt.repositories.etmpAgentRepository

  def verification(groupId: String, regimeId: String, affinityGroup: AffinityGroup) = Action.async { request =>
    val gId = GroupId(groupId)
    val rId = RegimeId(regimeId)
    affinityGroup match {
      case Agent => verify[AgentRegistration](gId, rId, request)
      case _ => verify[IndividualRegistration](gId, rId, request)
    }
  }

  private def verify[A: Verification: VerificationRepo](groupId: GroupId, regimeId: RegimeId, request: Request[AnyContent]) = {

    implicit val r = request // we need to get ExecutionContext from request by MdcLoggingExecutionContext means

    registrationService.verify[A](groupId, regimeId) map (response => Ok(Json.toJson(response)))
  }

  def prepopulation(groupId: String, regimeId: String, affinityGroup: AffinityGroup) = Action.async { implicit request =>
    registrationService.prepopulation(groupId, regimeId).map {
      case registration :: Nil =>
        NotFound
      // Ok(Json.toJson(registration))
      case Nil =>
        Logger.warn(s"Prepopulation data not found for groupId: $groupId and regimeId: $regimeId")
        NotFound
      case registration :: xs =>
        Logger.warn(s"More than one prepopulation data found for groupId: $groupId and regimeId: $regimeId")
        NotFound
      // Ok(Json.toJson(registration))
    }
  }

  def register() = Action.async(parse.json) { implicit request =>
    request.body.validate[RegisterRequest] match {
      case JsSuccess(req, _) =>
        registrationService.register(req).map(response => Ok(Json.toJson(response)))
      case JsError(jsonErrors) =>
        Logger.debug(s"incorrect request: ${jsonErrors} ")
        Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(jsonErrors))))
    }
  }

  def registerAgent() = Action.async(parse.json) { implicit request =>
    request.body.validate[RegisterAgentRequest] match {
      case JsSuccess(req, _) =>
        registrationService.register(req).map(response => Ok(Json.toJson(response)))
      case JsError(jsonErrors) =>
        Logger.debug(s"incorrect request: ${jsonErrors} ")
        Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(jsonErrors))))
    }
  }
}
