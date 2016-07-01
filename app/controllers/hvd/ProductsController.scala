package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.hvd.{Tobacco, Alcohol, Hvd, Products}
import views.html.hvd.products

import scala.concurrent.Future

trait ProductsController extends BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[Products] = (for {
              hvd <- response
              products <- hvd.products
            } yield Form2[Products](products)).getOrElse(EmptyForm)
            Ok(products(form, edit))
        }
    }
  }

  def post(edit : Boolean = false) = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        Form2[Products](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(products(f, edit)))
          case ValidForm(_, data) => {
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              _ <- dataCacheConnector.save[Hvd](Hvd.key,
                hvd.products(data)
              )
            } yield edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => {
                if (data.items.contains(Alcohol) | data.items.contains(Tobacco)) {
                  Redirect(routes.CashPaymentController.get())
                } else {
                  Redirect(routes.ProductsController.get())
                }
              }
            }
          }
        }
    }
  }
}

object ProductsController extends ProductsController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
