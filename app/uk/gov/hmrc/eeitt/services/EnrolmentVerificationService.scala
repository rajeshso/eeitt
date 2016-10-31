package uk.gov.hmrc.eeitt.services

import uk.gov.hmrc.eeitt.model.EnrolmentVerificationResponse.{ INCORRECT_ARN, INCORRECT_ARN_FOR_CLIENT, INCORRECT_POSTCODE, INCORRECT_REGIME, MISSING_ARN, RESPONSE_NOT_FOUND, RESPONSE_OK, MULTIPLE_FOUND }
import uk.gov.hmrc.eeitt.model._
import uk.gov.hmrc.eeitt.repositories.{ EnrolmentRepository, enrolmentRepository }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait EnrolmentVerificationService {

  def enrolmentRepo: EnrolmentRepository

  def verify(enrolmentRequest: EnrolmentVerificationRequest): Future[EnrolmentVerificationResponse] =
    enrolmentRepo.lookupEnrolment(enrolmentRequest.registrationNumber).flatMap {
      case Nil => Future.successful(RESPONSE_NOT_FOUND)
      case x :: Nil => doVerify(enrolmentRequest, x)
      case x :: xs => Future.successful(MULTIPLE_FOUND)
    }

  private def doVerify(request: EnrolmentVerificationRequest, enrolment: Enrolment): Future[EnrolmentVerificationResponse] =
    (request, enrolment) match {
      case DifferentPostcodes() => Future.successful(INCORRECT_POSTCODE)
      case DifferentFormTypes() => Future.successful(INCORRECT_REGIME)
      case DifferentArns() if request.isAgent => incorrectArnResponse(request)
      case _ => Future.successful(RESPONSE_OK)
    }

  private def incorrectArnResponse(request: EnrolmentVerificationRequest): Future[EnrolmentVerificationResponse] =
    request.arn match {
      case _ if request.arn.isEmpty => Future.successful(MISSING_ARN)
      case arn =>
        enrolmentRepo.getEnrolmentsWithArn(arn) map {
          case Nil => INCORRECT_ARN
          case xs => INCORRECT_ARN_FOR_CLIENT
        }
    }

  object DifferentPostcodes {
    def unapply(p: (EnrolmentVerificationRequest, Enrolment)): Boolean = p match {
      case (r, e) => (e.livesInTheUk && (norm(r.postcode) != norm(e.postcode))) || (r.livesInTheUk != e.livesInTheUk)
    }
    private def norm(p: String) = p.trim.toUpperCase.replaceAll("\\s", "")
  }

  object DifferentFormTypes {
    def unapply(p: (EnrolmentVerificationRequest, Enrolment)): Boolean = p match {
      case (r, e) => r.formTypeRef != e.formTypeRef
    }
  }

  object DifferentArns {
    def unapply(p: (EnrolmentVerificationRequest, Enrolment)): Boolean = p match {
      case (r, e) => r.arn != e.arn
    }
  }

}

object EnrolmentVerificationService extends EnrolmentVerificationService {
  lazy val enrolmentRepo = enrolmentRepository
}
