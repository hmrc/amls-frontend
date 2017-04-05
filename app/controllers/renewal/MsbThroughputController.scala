package controllers.renewal

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, HighValueDealing}
import models.renewal.MsbThroughput
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.msb_total_throughput

import scala.concurrent.Future

class MsbThroughputController @Inject()
(
  val authConnector: AuthConnector,
  renewals: RenewalService,
  dataCacheConnector: DataCacheConnector
) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        val maybeResult = for {
          renewal <- OptionT(renewals.getRenewal)
          throughput <- OptionT.fromOption[Future](renewal.msbThroughput)
        } yield {
          Ok(msb_total_throughput(Form2[MsbThroughput](throughput), edit))
        }

        maybeResult getOrElse Ok(msb_total_throughput(EmptyForm, edit))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        Form2[MsbThroughput](request.body) match {
          case form: InvalidForm => Future.successful(BadRequest(msb_total_throughput(form, edit)))
          case ValidForm(_, model) =>
            val maybeResult = for {
              renewal <- OptionT(renewals.getRenewal)
              _ <- OptionT.liftF(renewals.updateRenewal(renewal.msbThroughput(model)))
              cache <- OptionT(dataCacheConnector.fetchAll)
              businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
            } yield {
              Redirect(getNextPage(businessMatching, edit))
            }

            maybeResult.getOrElse(Redirect(routes.SummaryController.get()))
        }
  }

  private def getNextPage(businessMatching: BusinessMatching, edit: Boolean) =
    (businessMatching, edit) match {
      case (_, true) => routes.SummaryController.get()
      case (bm, _) => bm.activities match {
        case Some(x) if x.businessActivities.contains(HighValueDealing) => routes.PercentageOfCashPaymentOver15000Controller.get()
        case _ => routes.SummaryController.get()
      }
    }

}
