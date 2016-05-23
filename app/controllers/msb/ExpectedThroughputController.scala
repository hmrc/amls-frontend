package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{MoneyServiceBusiness, ExpectedThroughput}
import views.html.msb.expected_throughput

import scala.concurrent.Future

trait ExpectedThroughputController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[ExpectedThroughput] = (for {
            msb <- response
            expectedThroughput <- msb.throughput
          } yield Form2[ExpectedThroughput](expectedThroughput)).getOrElse(EmptyForm)
          Ok(expected_throughput(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedThroughput](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(expected_throughput(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.throughput(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.SummaryController.get())
          }
      }
    }
  }
}

object ExpectedThroughputController extends ExpectedThroughputController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
