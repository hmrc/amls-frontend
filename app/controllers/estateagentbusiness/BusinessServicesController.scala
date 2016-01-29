package controllers.estateagentbusiness

import akka.event.slf4j.Logger
import config.AMLSAuthConnector
import controllers.BaseController
import forms.{Form2, EmptyForm}
import models.estateagentbusiness.Service

import scala.concurrent.Future

trait BusinessServicesController extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.business_servicess_EAB(EmptyForm, edit)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      println("----------------------------------------------------------")
      println(request.body.asFormUrlEncoded)
      println("----------------------------------------------------------")
      println(Form2[Seq[Service]](request.body))
      println("----------------------------------------------------------")

      /*Form2[Service](request.body) match {
        case _ => print(request.body)
          Future.successful(Ok)
      }

*/
      Future.successful(Ok)
  }

}

object BusinessServicesController extends BusinessServicesController {
  override val authConnector = AMLSAuthConnector
}