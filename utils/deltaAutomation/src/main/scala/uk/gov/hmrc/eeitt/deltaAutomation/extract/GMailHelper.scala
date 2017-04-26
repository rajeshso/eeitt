package uk.gov.hmrc.eeitt.deltaAutomation.extract

import java.io.File
import java.util.Properties
import javax.activation.{ DataHandler, FileDataSource }
import javax.mail.Session
import javax.mail.internet.{ InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart }

import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.{ Message, ModifyMessageRequest }
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._
import scala.language.implicitConversions

trait GMailHelper extends GoogleAuthService {

  val logger: Logger
  private val userId = "me"
  protected val gMailService: Gmail

  protected def getMessageId(message: Message): String = {
    message.getId
  }

  protected def getLabels(id: String): List[String] = {
    val messages = gMailService.users().messages().get(userId, id).setFormat("full").execute()
    messages.getLabelIds.asScala.toList
  }

  protected def getSender(id: String): String = {
    val messages = gMailService.users().messages().get(userId, id).setFormat("full").execute()
    val messageList = messages.getPayload.getHeaders.asScala
    messageList.filter(header => header.getName == "From").head.getValue
  }

  protected def markMessageAsRead(id: String): Unit = {
    val mods = new ModifyMessageRequest().setRemoveLabelIds(List("UNREAD").asJava)
    val result = gMailService.users.messages().modify(userId, id, mods).execute()
  }

  protected def isValidEmail(id: String): Boolean = {
    val labels: List[String] = getLabels(id)
    val sender: String = getSender(id)
    labels.contains(config.getString("GMail.Helper.Labels")) && labels.contains("UNREAD") && sender == config.getString("GMail.Helper.Sender")
  }

  protected def createDeltaMessage(logFile: File, masterFile: File, result: String): MimeMessage = {
    val props = new Properties()
    val session = Session.getDefaultInstance(props, null)
    val email = new MimeMessage(session)
    email.setFrom(new InternetAddress(config.getString("GMail.Helper.Email")))
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(config.getString("GMail.Helper.Email")))
    email.setSubject(config.getString("GMail.Content.Subject"))
    email.setText(config.getString(s"GMail.Content.$result"))
    email.setContent(createBodyWithAttachment(logFile, masterFile))
    email
  }

  protected def createBodyWithAttachment(firstFile: File, secondFile: File): MimeMultipart = {
    val firstMimeBodyPart = new MimeBodyPart()
    val multipart = new MimeMultipart()
    val firstSource = new FileDataSource(firstFile)
    firstMimeBodyPart.setDataHandler(new DataHandler(firstSource))
    firstMimeBodyPart.setFileName(firstFile.getName)
    multipart.addBodyPart(firstMimeBodyPart)

    val secondMimeBodyPart = new MimeBodyPart()
    val secondSource = new FileDataSource(secondFile)
    secondMimeBodyPart.setDataHandler(new DataHandler(secondSource))
    secondMimeBodyPart.setFileName(secondFile.getName)
    multipart.addBodyPart(secondMimeBodyPart)
    multipart
  }

}
