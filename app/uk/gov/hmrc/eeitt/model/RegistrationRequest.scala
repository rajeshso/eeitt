package uk.gov.hmrc.eeitt.model
import play.api.libs.json._

case class RegistrationRequest(groupId: String, regimeId: String, registrationNumber: String, postcode: String)

object RegistrationRequest {
  implicit val registrationRequestFormat: Format[RegistrationRequest] = Json.format[RegistrationRequest]
}

