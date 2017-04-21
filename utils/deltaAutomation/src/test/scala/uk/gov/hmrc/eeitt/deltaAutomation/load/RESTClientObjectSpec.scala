package uk.gov.hmrc.eeitt.deltaAutomation.load

import scalaj.http._
import org.scalatest.{ FlatSpec, Matchers }
import uk.gov.hmrc.eeitt.deltaAutomation.transform.BusinessUser

class RESTClientObjectSpec extends FlatSpec with Matchers {
  "An incorrect URL" should "return an error message that there is a problem with the connection" in {
    RESTClientObject.process("dsfsd", BusinessUser) match {
      case Right(response) => fail("A HTTPResponse is unexpected")
      case Left(message) => message should be("Connection refused (Connection refused)")
    }
  }
}
