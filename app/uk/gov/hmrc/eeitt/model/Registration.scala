package uk.gov.hmrc.eeitt.model

import play.api.data.validation.ValidationError
import play.api.libs.json.{
  Json,
  OFormat,
  Format,
  JsResult,
  JsSuccess,
  JsError,
  JsString,
  JsValue
}

class GroupId(val value: String) extends AnyVal
class RegimeId(val value: String) extends AnyVal
class Arn(val value: String) extends AnyVal
class RegistrationNumber(val value: String) extends AnyVal
class Postcode(val value: String) extends AnyVal

case class RegistrationBusinessUser(groupId: GroupId, registrationNumber: RegistrationNumber, regimeId: RegimeId)
case class RegistrationAgent(groupId: GroupId, arn: Arn)

object RegistrationBusinessUser {
  implicit val oFormat: OFormat[RegistrationBusinessUser] = Json.format[RegistrationBusinessUser]
}

object RegistrationAgent {
  implicit val oFormat: OFormat[RegistrationAgent] = Json.format[RegistrationAgent]
}

object GroupId {
  def apply(value: String) = new GroupId(value)

  implicit val format: Format[GroupId] = ValueClassFormat.format(GroupId.apply)(_.value)
}

object RegimeId {
  def apply(value: String) = new RegimeId(value)

  implicit val format: Format[RegimeId] = ValueClassFormat.format(RegimeId.apply)(_.value)
}

object Arn {
  def apply(value: String) = new Arn(value.toUpperCase)

  implicit val format: Format[Arn] = ValueClassFormat.format(Arn.apply)(_.value)
}

object RegistrationNumber {
  def apply(value: String) = new RegistrationNumber(value.toUpperCase)

  implicit val format: Format[RegistrationNumber] = {
    val format = ValueClassFormat.format(RegistrationNumber.apply)(_.value)
    val reads = format.filter(ValidationError("Registration number must be 15 characters long"))(_.value.size == 15)

    Format(reads, format)
  }
}

object Postcode {
  def apply(value: String) = new Postcode(value)

  implicit val format: Format[Postcode] = ValueClassFormat.format(Postcode.apply)(_.value)
}

object ValueClassFormat {
  def format[A: Format](fromStringToA: String => A)(fromAToString: A => String) = {
    new Format[A] {
      def reads(json: JsValue): JsResult[A] = {
        json match {
          case JsString(str) => JsSuccess(fromStringToA(str))
          case unknown => JsError(s"JsString value expected, got: $unknown")
        }
      }
      def writes(a: A): JsValue = JsString(fromAToString(a))
    }
  }
}
