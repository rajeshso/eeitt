package uk.gov.hmrc.eeitt.deltaAutomation.extract

import java.io.{ File, InputStreamReader }

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{ GoogleAuthorizationCodeFlow, GoogleClientSecrets }
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.GmailScopes

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{ Failure, Success, Try }

class GoogleAuthService {

  protected val APPLICATION_NAME = "Delta Automation"
  protected val DATA_STORE_DIR: File = new File(".credentials")
  protected val JSON_FACTORY: JacksonFactory = JacksonFactory.getDefaultInstance
  private val SCOPES = Set(GmailScopes.MAIL_GOOGLE_COM, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY).asJava

  protected val HTTP_TRANSPORT: HttpTransport =
    Try(GoogleNetHttpTransport.newTrustedTransport()) match {
      case Success(x) => x
      case Failure(f) => throw new IllegalArgumentException("HTTP_TRANSPORT :- FAILED TO INITIALISE")
    }

  protected val DATA_STORE_FACTORY: FileDataStoreFactory =
    Try(new FileDataStoreFactory(DATA_STORE_DIR)) match {
      case Success(x) => x
      case Failure(f) => throw new IllegalArgumentException("DATA_STORE_FACTORY :- FAILED TO INITIALISE")
    }

  def authorise: Credential = {
    val in: InputStreamReader = scala.io.Source.fromFile("src/main/resources/client_secret.json").reader()
    val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, in)
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(DATA_STORE_FACTORY)
      .setAccessType("offline")
      .build
    new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user")
  }
}
