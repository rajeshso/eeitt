package uk.gov.hmrc.eeitt.deltaAutomation.services

import java.io.{File, InputStreamReader}

import com.typesafe.scalalogging.Logger
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets, GoogleCredential}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.{DataStoreFactory, FileDataStoreFactory}
import com.google.api.services.gmail.GmailScopes
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

class GoogleAuthService {

  val config : Config = ConfigFactory.load()
  protected val APPLICATION_NAME = config.getString("GMail.OAuth.ApplicationName")
  private val runningLocation = getClass.getResource("/auth/credentials")
  protected val DATA_STORE_DIR: File = new File(runningLocation.getPath.drop(5))
  protected val JSON_FACTORY: JacksonFactory = JacksonFactory.getDefaultInstance
  private val SCOPES = Set(GmailScopes.MAIL_GOOGLE_COM, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY).asJava

  protected val HTTP_TRANSPORT: HttpTransport =
    Try(GoogleNetHttpTransport.newTrustedTransport()) match {
      case Success(x) => x
      case Failure(f) => throw new IllegalArgumentException("HTTP_TRANSPORT :- FAILED TO INITIALISE")
    }

  protected val DATA_STORE_FACTORY: DataStoreFactory =
    Try(new FileDataStoreFactory(DATA_STORE_DIR)) match {
      case Success(x) => x
      case Failure(f) => throw new IllegalArgumentException(s"DATA_STORE_FACTORY :- FAILED TO INITIALISE ${f.getMessage}")
    }

  def authorise: Credential = {
    val client_secret = getClass.getResourceAsStream("/auth/client_secret.json")
    val client_secret_input_stream: InputStreamReader = scala.io.Source.fromInputStream(client_secret).reader()
    val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, client_secret_input_stream)
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(DATA_STORE_FACTORY)
      .setAccessType("offline")
      .build
    new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user")
  }
}
