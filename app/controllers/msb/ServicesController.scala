package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{MoneyServiceBusiness, MsbServices}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait ServicesController extends BaseController {

  def cache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      import play.api.data.mapping.forms.Writes._

//      cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
//        response =>
//
//          val form = (for {
//            msb <- response
//            services <- msb.msbServices
//          } yield Form2[MsbServices](services)).getOrElse(EmptyForm)
//
//          Ok(views.html.msb.services(form, edit))
//      }

      Future.successful(Ok("foo"))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      Future.successful(Ok("foo"))

//      Form2[MsbServices](request.body) match {
//        case f: InvalidForm =>
//          Future.successful(BadRequest(views.html.msb.services(f, edit)))
//        case ValidForm(_, data) =>
//          for {
//            msb <- cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
//            _ <- cache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
//              msb.msbServices(data)
//            )
//              // TODO: Go to next page
//          } yield Redirect(routes.ServicesController.get())
//      }
  }
}

object ServicesController extends ServicesController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val cache = DataCacheConnector
}
