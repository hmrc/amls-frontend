package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises.{MsbServices, TradingPremises}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

trait MSBServicesController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(tp) => {
          val form = tp.msbServices match {
            case Some(service) => Form2[MsbServices](service)
            case None => EmptyForm
          }
          Ok(views.html.tradingpremises.msb_services(form, index, edit))
        }
        case None => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.msb_services(f, index, edit)))
        case ValidForm(_, data) => {
          for {
            _ <- updateDataStrict[TradingPremises](index) {
              case Some(tp) => Some(tp.msbServices(data))
              case _ => Some(TradingPremises(msbServices = Some(data)))
            }
          } yield Redirect(routes.PremisesRegisteredController.get(index))
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }
}

object MSBServicesController extends MSBServicesController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
