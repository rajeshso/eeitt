package uk.gov.hmrc.eeitt.services

import play.api.libs.json.{ Json, JsObject }
import uk.gov.hmrc.eeitt.model._
import uk.gov.hmrc.eeitt.repositories._
import uk.gov.hmrc.eeitt.services.PostcodeValidator._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Show[A] {
  def apply(a: A): String
}

object Show {
  implicit val groupIdShow = new Show[GroupId] {
    def apply(groupId: GroupId): String = s"GroupId(${groupId.value})"
  }

  implicit val groupIdAndRegimeIdShow = new Show[(GroupId, RegimeId)] {
    def apply(groupIdAndRegimeId: (GroupId, RegimeId)): String = {
      val (groupId, regimeId) = groupIdAndRegimeId
      s"(GroupId(${groupId.value}), RegimeId(${regimeId.value}))"
    }
  }
}

trait PrepopulationData[A] {
  def apply(a: A): JsObject
}

object PrepopulationData {
  implicit val prepopBusinessUser = new PrepopulationData[RegistrationBusinessUser] {
    def apply(businessUser: RegistrationBusinessUser): JsObject = Json.obj("registrationNumber" -> businessUser.registrationNumber.value)
  }

  implicit val prepopAgent = new PrepopulationData[RegistrationAgent] {
    def apply(agent: RegistrationAgent): JsObject = Json.obj("arn" -> agent.arn.value)
  }
}

trait FindRegistration[A] {
  type Out
  def apply(a: A): Future[List[Out]]
}

object FindRegistration {

  type Aux[A, Out0] = FindRegistration[A] { type Out = Out0 }

  private def getFindRegistration[A, B](f: A => Future[List[B]]): FindRegistration.Aux[A, B] = {
    new FindRegistration[A] {
      type Out = B
      def apply(params: A): Future[List[B]] = f(params)
    }
  }

  implicit def businessUserByRequest(implicit repository: RegistrationRepository) = {
    getFindRegistration((r: RegisterBusinessUserRequest) => repository.findRegistrations(r.groupId, r.regimeId))
  }

  implicit def businessUserByGroupIdAndRegimeId(implicit repository: RegistrationRepository) = {
    getFindRegistration((t: (GroupId, RegimeId)) => repository.findRegistrations(t._1, t._2))
  }

  implicit def agentByRequest(implicit repository: RegistrationAgentRepository) = {
    getFindRegistration((r: RegisterAgentRequest) => repository.findRegistrations(r.groupId))
  }

  implicit def agentByGroupId(implicit repository: RegistrationAgentRepository) = {
    getFindRegistration((g: GroupId) => repository.findRegistrations(g))
  }
}

trait FindUser[A, B] {
  def apply(a: A): Future[List[B]]
}

object FindUser {

  private def getFindUser[A, B](f: A => Future[List[B]]): FindUser[A, B] = {
    new FindUser[A, B] {
      def apply(params: A): Future[List[B]] = f(params)
    }
  }

  implicit def agentExists(implicit repository: EtmpAgentRepository) = {
    getFindUser((r: RegisterAgentRequest) => repository.findByArn(r.arn))
  }

  implicit def businessUserExists(implicit repository: EtmpBusinessUsersRepository) = {
    getFindUser((r: RegisterBusinessUserRequest) => repository.findByRegistrationNumber(r.registrationNumber))
  }
}

trait AddRegistration[A] {
  def apply(a: A): Future[Either[String, Unit]]
}

object AddRegistration {

  private def getAddRegistration[A](f: A => Future[Either[String, Unit]]): AddRegistration[A] = {
    new AddRegistration[A] {
      def apply(params: A): Future[Either[String, Unit]] = f(params)
    }
  }

  implicit def agentRepo(implicit repository: RegistrationAgentRepository) = {
    getAddRegistration((r: RegisterAgentRequest) => repository.register(r))
  }

  implicit def businessUserRepo(implicit repository: RegistrationRepository) = {
    getAddRegistration((r: RegisterBusinessUserRequest) => repository.register(r))
  }
}

object RegistrationService {

  def register[A <: RegisterRequest, B: PostcodeValidator](
    registerRequest: A
  )(
    implicit
    findExistingRegistration: FindRegistration[A],
    addRegistration: AddRegistration[A],
    findKnownFact: FindUser[A, B]
  ): Future[RegistrationResponse] = {
    findKnownFact(registerRequest).flatMap {
      case etmpData :: maybeOtherUsers if postcodeValidOrNotNeeded(etmpData, registerRequest.postcode) =>
        findExistingRegistration(registerRequest).flatMap {
          case Nil => addRegistration(registerRequest).map {
            case Right(_) => RESPONSE_OK
            case Left(x) => Other(x)
          }
          case x :: Nil => Future.successful(RESPONSE_OK)
          case x :: xs => Future.successful(MULTIPLE_FOUND)
        }
      case _ =>
        registerRequest match {
          case _: RegisterAgentRequest => Future.successful(INCORRECT_KNOWN_FACTS_AGENTS)
          case _: RegisterRequest => Future.successful(INCORRECT_KNOWN_FACTS_BUSINESS_USERS)
        }
    }
  }

  def findRegistration[A](findParams: A)(
    implicit
    findRegistration: FindRegistration[A]
  ): Future[List[findRegistration.Out]] = {
    findRegistration(findParams)
  }
}
