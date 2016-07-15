package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{InvalidForm, ValidForm, EmptyForm, Form2}
import models.hvd.{ExciseGoods, Hvd}

import views.html.hvd.excise_goods

import scala.concurrent.Future

trait ExciseGoodsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[ExciseGoods] = (for {
              hvd <- response
              exciseGoods <- hvd.exciseGoods
            } yield Form2[ExciseGoods](exciseGoods)).getOrElse(EmptyForm)
            Ok(excise_goods(form, edit))
        }
    }
  }

  def post(edit: Boolean = false) = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request => {
        Form2[ExciseGoods](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(excise_goods(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              _ <- dataCacheConnector.save[Hvd](Hvd.key,
                hvd.exciseGoods(data)
              )
            } yield edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.HowWillYouSellGoodsController.get())
            }
        }
      }
    }
  }
}

object ExciseGoodsController extends ExciseGoodsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}