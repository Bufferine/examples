package com.redvblack.examples.comet


/**
 * Created by IntelliJ IDEA.
 * User: bufferine
 * Date: 17-Nov-2010
 * Time: 7:30:45 PM
 * To change this template use File | Settings | File Templates.
 * Modded by Buff
 */

import scala.xml._
import net.liftweb.http.js.JsCmds._
import net.liftweb.actor.LiftActor
import net.liftweb.http._
import js.JE._
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.util._
import com.redvblack.examples.model.User


class UserClient extends CometActor with CometListener {
  override protected def devMode = true
  private var emailBound:Boolean = false;
  private var userLine: NodeSeq = Nil
  def registerWith = UserServer

  override def lowPriority = {
    case UserPropertyUpdate(userId: Long, "firstName", value: String) => {
      partialUpdate(SetValById("user_firstName_" + userId, Str(value)))
    }
    case UserPropertyUpdate(userId: Long, "lastName", value: String) => {
      partialUpdate(SetValById("user_lastName_" + userId, Str(value)))
    }
    case UserPropertyUpdate(userId: Long, "email", value: String) => {
      if (emailBound == true){
        println("Email bound - sending message")
        SetValById("user_email_" + userId, Str(value))
    }else{
        println("email not bound, so not sending update" )
    }
    }
  }


  def render = ".user *" #> (users _)


  private def users(xml: NodeSeq): NodeSeq = {
    userLine = xml

    for{
      user: User <- com.redvblack.examples.model.User.users
      node <- doLine(user)
    } yield node


  }


  def doLine(user:User) = {

      (".user_firstName" #> {
      SHtml.ajaxText(user.firstName.is, (value) => {
        UserServer ! UserPropertyUpdate(user.id, "firstName", value.toString)
        Noop
      }, ("id", "user_firstName_" + user.id))
    }

            & ".user_lastName" #> {
      SHtml.ajaxText(user.lastName.is, (value) => {
        UserServer ! UserPropertyUpdate(user.id, "lastName", value.toString)
        Noop
      }, ("id", "user_lastName_" + user.id))
    }
        
            & ".user_email" #> {
        emailBound = true
      SHtml.ajaxText(user.email.is, (value) => {
        UserServer ! UserPropertyUpdate(user.id, "email", value.toString)
        Noop
      }, ("id", "user_email_" + user.id))
    }
    ).apply(userLine)
  }
}

  object UserServer extends LiftActor with ListenerManager {

      var userToUpdate: Box[UserUpdate] = Empty

  def createUpdate = {
    userToUpdate match {
      case Empty =>
      case Full(user) => {user}
      case _ =>
    }
  }

    def updateUser(userId: Long, property: String, value: String): Boolean = {
      val foundUser = User.find(userId)
      foundUser match {
        case Full(user) => updateUser(user, property, value)
        case Empty => false
        case _ => false
      }
    }
    /*
     override def fieldOrder = List(id, firstName, lastName, email,
  locale, timezone, password, textArea)
    */
    def updateUser(user: User, property: String, value: String): Boolean = {
      property match {
        case "firstName" => user.firstName.set(value); user.save; userToUpdate = Box(UserPropertyUpdate(user.id.is, "firstName", user.firstName.is)); true
        case "lastName" => user.lastName.set(value); user.save; userToUpdate = Box(UserPropertyUpdate(user.id.is, "lastName", user.lastName.is)); true
        case "email" => user.email.set(value); user.save; userToUpdate = Box(UserPropertyUpdate(user.id.is, "email", user.email.is)); true
      }
    }


    override def lowPriority = {
      case UserPropertyUpdate(userId: Long, property: String, value: String) => {
        if (updateUser(userId, property, value)) {
          updateListeners()
        }
      }
    }
  }

  sealed trait UserUpdate

  object UserUpdate {
    implicit def strToMsg(userId: Long, what: String, value: String): UserUpdate =
      new UserPropertyUpdate(userId, what, value)
  }
  

  final case class UserPropertyUpdate(userId: Long, what: String, value: String) extends UserUpdate
