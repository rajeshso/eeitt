package uk.gov.hmrc.eeitt.deltaAutomation.extract

import java.io.File
import java.util.Properties
import javax.activation.{ DataHandler, FileDataSource }
import javax.mail.Session
import javax.mail.internet.{ InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart }

import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.{ Message, ModifyMessageRequest }

import scala.collection.JavaConverters._
import scala.language.implicitConversions

trait GMailHelper extends GoogleAuthService {

  protected val gMailService: Gmail = {
    val credential: Credential = authorise
    new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
      .setApplicationName(APPLICATION_NAME)
      .build
  }

  protected def getMessageId(message: Message): String = {
    message.getId
  }

  protected def getLabels(id: String): List[String] = {
    val userId = "me"
    val messages = gMailService.users().messages().get(userId, id).setFormat("full").execute()
    messages.getLabelIds.asScala.toList
  }

  protected def getSender(id: String): String = {
    val userId = "me"
    val messages = gMailService.users().messages().get(userId, id).setFormat("full").execute()
    val messageList = messages.getPayload.getHeaders.asScala
    messageList.filter(header => header.getName == "From").head.getValue
  }

  protected def markMessageAsRead(id: String): Unit = {
    val userId = "me"
    val mods = new ModifyMessageRequest().setRemoveLabelIds(List("UNREAD").asJava)
    val result = gMailService.users.messages().modify(userId, id, mods).execute()
  }

  protected def isValidEmail(id: String): Boolean = {
    val labels: List[String] = getLabels(id)
    val sender: String = getSender(id)
    labels.contains("Label_1") && labels.contains("UNREAD") && sender == "<Sharlena.Campbell@hmrc.gsi.gov.uk>"
  }

  protected def createDeltaMessage(file: File): MimeMessage = {
    val props = new Properties()
    val session = Session.getDefaultInstance(props, null)
    val email = new MimeMessage(session)
    email.setFrom(new InternetAddress("service_eeitt.digital_ddcw@digital.hmrc.gov.uk"))
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress("service_eeitt.digital_ddcw@digital.hmrc.gov.uk"))
    email.setSubject("DeltaAutomation Clean Data")
    email.setContent(createBodyWithAttachment(file))
    email
  }

  protected def createBodyWithAttachment(file: File): MimeMultipart = {
    val mimeBodyPart = new MimeBodyPart()
    val multipart = new MimeMultipart()
    val source = new FileDataSource(file)
    mimeBodyPart.setDataHandler(new DataHandler(source))
    mimeBodyPart.setFileName(file.getName)
    mimeBodyPart.setContent("Please find Attached Clean deltas", "text/plain")
    multipart.addBodyPart(mimeBodyPart)
    multipart
  }

}
