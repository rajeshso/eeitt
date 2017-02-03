package uk.gov.hmrc.eeitt.controllers

import akka.stream.Materializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.Inside
import play.api.Environment
import play.api.http.Status
import play.api.i18n.{ Messages, MessagesApi, DefaultLangs, DefaultMessagesApi }
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.eeitt.typeclasses.HmrcAudit
import uk.gov.hmrc.eeitt.{ EtmpFixtures, RegistrationFixtures, TypeclassFixtures }
import uk.gov.hmrc.eeitt.model.{ VerificationResponse, _ }
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{ UnitSpec, WithFakeApplication }
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.eeitt.checks._
import uk.gov.hmrc.eeitt.ApplicationComponentsOnePerTest

class RegistrationControllerSpec extends UnitSpec with ApplicationComponentsOnePerTest with Inside with EtmpFixtures with RegistrationFixtures with TypeclassFixtures with ScalaFutures with MockFactory {

  implicit val materializer: Materializer = fakeApplication.materializer

  val messagesApiDefault = new DefaultMessagesApi(Environment.simple(), fakeApplication.configuration, new DefaultLangs(fakeApplication.configuration))

  object TestRegistrationController extends RegistrationControllerHelper {
    val messagesApi = messagesApiDefault
  }

  "GET /group-identifier/:gid/regimes/:regimeid/verification" should {
    "return 200 and is allowed for successful registration lookup where regime is authorised" in {
      val fakeRequest = FakeRequest()
      implicit val a = FindRegistrationTC.response(List(testRegistrationBusinessUser())).noChecks[(GroupId, RegimeId)]
      val action = TestRegistrationController.verify((GroupId("1"), RegimeId("ZZ")))
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe toJson(VerificationResponse(true))
    }

    "return 200 and is not allowed for successful registration lookup where regime is not authorised" in {
      val fakeRequest = FakeRequest()
      implicit val a = FindRegistrationTC.response(List.empty[RegistrationBusinessUser]).noChecks[(GroupId, RegimeId)]
      val action = TestRegistrationController.verify((GroupId("1"), RegimeId("ZZ")))
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe toJson(VerificationResponse(false))
    }

    "return 200 and is not allowed for a lookup which returned multiple registration instances" in {
      val fakeRequest = FakeRequest()
      implicit val a = FindRegistrationTC.response(List(testRegistrationBusinessUser(), testRegistrationBusinessUser())).noChecks[(GroupId, RegimeId)]

      val action = TestRegistrationController.verify((GroupId("1"), RegimeId("ZZ")))
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe toJson(VerificationResponse(false))
    }

    "return 200 and is allowed for successful registration lookup of agent" in {
      val fakeRequest = FakeRequest()
      implicit val a = FindRegistrationTC.response(List(testRegistrationAgent())).noChecks[GroupId]
      val action = TestRegistrationController.verify(GroupId("1"))
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe toJson(VerificationResponse(true))
    }
  }

  "POST /eeitt-auth/register" should {

    "Register business user and send audit change" in {

      val hmrcAuditCheck = mock[AuditCheck]
      val findUserCheck = mock[FindUserCheck]
      val addRegistrationCheck = mock[AddRegistrationCheck]
      val findRegistrationCheck = mock[FindRegistrationCheck]

      val fakeRequest = FakeRequest(Helpers.POST, "/register").withBody(toJson(RegisterBusinessUserRequest(GroupId("1"), RegistrationNumber("1234567890ABCDE"), Some(Postcode("SE39EPX")))))

      val messages = messagesApiDefault.preferred(fakeRequest)

      implicit val a = AddRegistrationTC
        .callCheck(addRegistrationCheck)
        .response(Right(()))
        .noChecks[RegisterBusinessUserRequest]

      implicit val b = FindRegistrationTC
        .callCheck(findRegistrationCheck)
        .noChecks[RegisterBusinessUserRequest]

      implicit val c = FindUserTC
        .callCheck(findUserCheck)
        .response(List(testEtmpBusinessUser()))
        .withChecks { req: RegisterBusinessUserRequest =>
          inside(req) {
            case RegisterBusinessUserRequest(groupId, registrationNumber, postcode) =>
              groupId.value should be("1")
              registrationNumber.value should be("1234567890ABCDE")
              convertOptionToValuable(postcode).value.value should be("SE39EPX")
          }
        }

      implicit val d = HmrcAuditTC
        .callCheck(hmrcAuditCheck)
        .withChecks { ad =>
          ad.path should be("/register")
          ad.postcode.map(_.value) should be(Some("SE39EPX"))

          ad.tags should contain("user-type" -> "business-user")
          ad.tags should contain("registration-number" -> "1234567890ABCDE")
          ad.tags should contain("group-id" -> "1")
          ad.tags should contain("regime-id" -> "34")
        }

      (hmrcAuditCheck.call _).expects().once
      (findUserCheck.call _).expects().once
      (addRegistrationCheck.call _).expects().once
      (findRegistrationCheck.call _).expects().once

      val action = TestRegistrationController.register[RegisterBusinessUserRequest, EtmpBusinessUser]
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe RESPONSE_OK.toJson(messages)
    }

    "Register agent and send audit change" in {

      val hmrcAuditCheck = mock[AuditCheck]
      val findUserCheck = mock[FindUserCheck]
      val addRegistrationCheck = mock[AddRegistrationCheck]
      val findRegistrationCheck = mock[FindRegistrationCheck]

      val fakeRequest = FakeRequest(Helpers.POST, "/register").withBody(toJson(RegisterAgentRequest(GroupId("1"), Arn("1234567890ABCDE"), Some(Postcode("SE39EPX")))))

      val messages = messagesApiDefault.preferred(fakeRequest)

      implicit val a = AddRegistrationTC
        .callCheck(addRegistrationCheck)
        .response(Right(()))
        .noChecks[RegisterAgentRequest]

      implicit val b = FindRegistrationTC
        .callCheck(findRegistrationCheck)
        .noChecks[RegisterAgentRequest]

      implicit val c = FindUserTC
        .callCheck(findUserCheck)
        .response(List(testEtmpAgent()))
        .withChecks { req: RegisterAgentRequest =>
          inside(req) {
            case RegisterAgentRequest(groupId, arn, postcode) =>
              groupId.value should be("1")
              arn.value should be("1234567890ABCDE")
              convertOptionToValuable(postcode).value.value should be("SE39EPX")
          }
        }

      implicit val d = HmrcAuditTC
        .callCheck(hmrcAuditCheck)
        .withChecks { ad =>
          ad.path should be("/register")
          ad.postcode.map(_.value) should be(Some("SE39EPX"))

          ad.tags should contain("user-type" -> "agent")
          ad.tags should contain("arn" -> "1234567890ABCDE")
          ad.tags should contain("group-id" -> "1")
        }

      (hmrcAuditCheck.call _).expects().once
      (findUserCheck.call _).expects().once
      (addRegistrationCheck.call _).expects().once
      (findRegistrationCheck.call _).expects().once

      val action = TestRegistrationController.register[RegisterAgentRequest, EtmpAgent]
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe RESPONSE_OK.toJson(messages)
    }

    "return 200 and error if submitted known facts are different than stored known facts about business user" in {

      val hmrcAuditCheck = mock[AuditCheck]

      val fakeRequest = FakeRequest(Helpers.POST, "/register").withBody(toJson(RegisterBusinessUserRequest(GroupId("1"), RegistrationNumber("1234567890ABCDE"), Some(Postcode("SE39EPX")))))

      val messages = messagesApiDefault.preferred(fakeRequest)

      implicit val a = AddRegistrationTC.response(Right(())).noChecks[RegisterBusinessUserRequest]
      implicit val b = FindRegistrationTC.response(List.empty[RegisterBusinessUserRequest]).noChecks[RegisterBusinessUserRequest]

      implicit val c = FindUserTC.response(List.empty[EtmpBusinessUser]).withChecks { req: RegisterBusinessUserRequest =>
        inside(req) {
          case RegisterBusinessUserRequest(groupId, registrationNumber, postcode) =>
            groupId.value should be("1")
            registrationNumber.value should be("1234567890ABCDE")
            convertOptionToValuable(postcode).value.value should be("SE39EPX")
        }
      }

      implicit val d = HmrcAuditTC.callCheck(hmrcAuditCheck).noChecks

      (hmrcAuditCheck.call _).expects().never

      val action = TestRegistrationController.register[RegisterBusinessUserRequest, EtmpBusinessUser]
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe INCORRECT_KNOWN_FACTS_BUSINESS_USERS.toJson(messages)
    }

    "return 200 and error if submitted known facts are different than stored known facts about agent" in {
      val fakeRequest = FakeRequest(Helpers.POST, "/register").withBody(toJson(RegisterAgentRequest(GroupId("1"), Arn("12LT009"), Some(Postcode("SE39EPX")))))
      val messages = messagesApiDefault.preferred(fakeRequest)

      implicit val a = AddRegistrationTC.response(Right(())).noChecks[RegisterAgentRequest]
      implicit val b = FindRegistrationTC.response(List.empty[RegisterAgentRequest]).noChecks[RegisterAgentRequest]

      implicit val c = FindUserTC.response(List.empty[EtmpAgent]).withChecks { req: RegisterAgentRequest =>
        inside(req) {
          case RegisterAgentRequest(groupId, arn, postcode) =>
            groupId.value should be("1")
            arn.value should be("12LT009")
            convertOptionToValuable(postcode).value.value should be("SE39EPX")
        }
      }

      implicit val d = new HmrcAudit[AuditData] {
        override def apply(ad: AuditData): HeaderCarrier => Unit = hc => {
          ()
        }
      }

      val action = TestRegistrationController.register[RegisterAgentRequest, EtmpAgent]
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe INCORRECT_KNOWN_FACTS_AGENTS.toJson(messages)
    }

    "return 400 and error if submitted known facts are different than stored known facts about agent" in {
      val fakeRequest = FakeRequest(Helpers.POST, "/register").withBody(Json.obj("invalid-request-json" -> "dummy"))

      implicit val a = AddRegistrationTC.response(Right(())).noChecks[RegisterAgentRequest]
      implicit val b = FindRegistrationTC.response(List.empty[RegisterAgentRequest]).noChecks[RegisterAgentRequest]
      implicit val c = FindUserTC.response(List.empty[EtmpAgent]).noChecks[RegisterAgentRequest]
      implicit val d = HmrcAuditTC.noChecks

      val action = TestRegistrationController.register[RegisterAgentRequest, EtmpAgent]
      val result = action(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "Prepopulation" should {
    "return arn for agent" in {
      val fakeRequest = FakeRequest()
      val agent = testRegistrationAgent()
      implicit val a = FindRegistrationTC.response(List(agent)).noChecks[GroupId]
      val action = TestRegistrationController.prepopulate[GroupId, RegistrationAgent](GroupId("1"))
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe Json.obj("arn" -> agent.arn.value)
    }

    "return 404 where arn for agent is not found in db" in {
      val fakeRequest = FakeRequest()
      implicit val a = FindRegistrationTC.response(List.empty[RegistrationAgent]).noChecks[GroupId]
      val action = TestRegistrationController.prepopulate[GroupId, RegistrationAgent](GroupId("1"))
      val result = action(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
      bodyOf(await(result)) shouldBe ""
    }

    "return first arn of first agent if there are more agents in db" in {
      val fakeRequest = FakeRequest()
      val agent1 = testRegistrationAgent()
      val agent2 = testRegistrationAgent()
      implicit val a = FindRegistrationTC.response(List(agent1, agent2)).noChecks[GroupId]
      val action = TestRegistrationController.prepopulate[GroupId, RegistrationAgent](GroupId("1"))
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe Json.obj("arn" -> agent1.arn.value)
    }

    "return registrationNumber for business user" in {
      val fakeRequest = FakeRequest()
      val businessUser = testRegistrationBusinessUser()
      implicit val a = FindRegistrationTC.response(List(businessUser)).noChecks[(GroupId, RegimeId)]
      val action = TestRegistrationController.prepopulate[(GroupId, RegimeId), RegistrationBusinessUser]((GroupId("1"), RegimeId("ZZ")))
      val result = action(fakeRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(await(result)) shouldBe Json.obj("registrationNumber" -> businessUser.registrationNumber.value)
    }
  }
}
