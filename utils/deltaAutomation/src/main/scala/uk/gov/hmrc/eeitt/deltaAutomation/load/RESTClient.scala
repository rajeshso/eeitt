package uk.gov.hmrc.eeitt.deltaAutomation.load

import java.net.ConnectException

import com.typesafe.config.{ Config, ConfigFactory }
import uk.gov.hmrc.eeitt.deltaAutomation.transform.{ AgentUser, BusinessUser, Locations, UnsupportedUser, User }

import scala.util.{ Failure, Success, Try }
import scalaj.http._

trait RESTClient {

  lazy val conf: Config = ConfigFactory.load()
  lazy val username = conf.getString("dryrun.user")
  lazy val password = conf.getString("dryrun.password")
  lazy val requestedWith = conf.getString("dryrun.xrequestedwith")
  lazy val agenturl = conf.getString("dryrun.url.agent")
  lazy val businessurl = conf.getString("dryrun.url.business")

  //logger.debug(s"username = ${username} password = ${password} requestedWith = ${requestedWith} agenturl = ${agenturl} businessurl = ${businessurl}")

  def dryRun(payLoadString: String, user: User, xrequestedwith: String, username: String, password: String): Either[String, HttpResponse[String]] = {
    user match {
      case BusinessUser => dryRun(payLoadString, businessurl, requestedWith, username, password)
      case AgentUser => dryRun(payLoadString, agenturl, requestedWith, username, password)
      case UnsupportedUser => Left("The user is unsupported")
      case _ => Left("The user is unsupported")
    }
  }

  def dryRun(payLoadString: String, url: String, xrequestedwith: String, username: String, password: String): Either[String, HttpResponse[String]] = {
    Try(Http(url)
      .header("Content-Type", "application/json")
      .header("Charset", "UTF-8")
      .header("x-requested-with", xrequestedwith)
      .auth(username, password)
      .postData(payLoadString.getBytes("UTF-8"))
      .option(HttpOptions.readTimeout(0)).asString) match {
      case Success(respo: HttpResponse[String]) => Right(respo)
      case Failure(exception: Throwable) => Left(exception.getMessage)
    }
  }
}

object RESTClientObject extends RESTClient {
  def process(payLoadString: String, user: User): Either[String, HttpResponse[String]] = dryRun(payLoadString, user, requestedWith, username, password)
}
