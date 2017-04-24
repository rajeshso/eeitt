package uk.gov.hmrc.eeitt.deltaAutomation.extract

import java.io.{ File, InputStreamReader }

import com.typesafe.scalalogging.Logger
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{ GoogleAuthorizationCodeFlow, GoogleClientSecrets, GoogleCredential }
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.{ DataStoreFactory, FileDataStoreFactory }
import com.google.api.services.gmail.GmailScopes
import com.typesafe.config.{ Config, ConfigFactory }
import uk.gov.hmrc.eeitt.deltaAutomation.Errors.FailureReason
import uk.gov.hmrc.eeitt.deltaAutomation.transform.Locations._

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{ Failure, Success, Try }
import scalaz.{ -\/, \/, \/- }
class GoogleAuthService {

  val config: Config = ConfigFactory.load()

  protected val APPLICATION_NAME: String = config.getString("GMail.OAuth.ApplicationName")
  protected val DATA_STORE_DIR: File = new File(getPath("/auth/credentials"))
  protected val JSON_FACTORY: JacksonFactory = JacksonFactory.getDefaultInstance
  private val SCOPES = Set(GmailScopes.MAIL_GOOGLE_COM, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY).asJava

  protected val HTTP_TRANSPORT: FailureReason \/ HttpTransport =
    Try(GoogleNetHttpTransport.newTrustedTransport()) match {
      case Success(x) => \/-(x)
      case Failure(f) =>
        -\/(FailureReason("HTTP_Transport did not initialise"))
    }

  protected val DATA_STORE_FACTORY: FailureReason \/ DataStoreFactory =
    Try(new FileDataStoreFactory(DATA_STORE_DIR)) match {
      case Success(x) => \/-(x)
      case Failure(f) =>
        -\/(FailureReason("DataStoreFactory did not Initialise"))
    }

  val authorise: FailureReason \/ Credential = {
    for {
      dataStore <- DATA_STORE_FACTORY
      http <- HTTP_TRANSPORT
    } yield {
      val client_secret = getClass.getResourceAsStream("/auth/client_secret.json")
      val client_secret_input_stream: InputStreamReader = scala.io.Source.fromInputStream(client_secret).reader()
      val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, client_secret_input_stream)
      val flow = new GoogleAuthorizationCodeFlow.Builder(http, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(dataStore)
        .setAccessType("offline")
        .build
      new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user")
    }
  }

}
