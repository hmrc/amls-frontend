package controllers.tradingpremises

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.{PremisesRegistered, PersonRegistered, VATRegistered, ResponsiblePeople}
import models.tradingpremises.TradingPremises
import utils.RepeatingSection


import scala.concurrent.Future

trait PremisesRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Seq[TradingPremises]](TradingPremises.key) map {
          case Some(data) => Ok(views.html.tradingpremises.premises_registered(EmptyForm, data.size))
          case _ => Ok(views.html.tradingpremises.premises_registered(EmptyForm, index))
        }
    }


  def post(index: Int) =
     Authorised.async {
        implicit authContext => implicit request =>
          Form2[PremisesRegistered](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.tradingpremises.premises_registered(f, index)))
            case ValidForm(_, data) =>
               data.registerAnotherPremises match {
                case true => Future.successful(Redirect(routes.TradingPremisesAddController.get(false )))
                case false => Future.successful(Redirect(routes.SummaryController.get()))
              }
          }
      }

}

object PremisesRegisteredController extends PremisesRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
