package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.hvd.{Alcohol, Hvd, Products, Tobacco}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved}
import services.StatusService
import utils.DateOfChangeHelper
import views.html.hvd.products

import scala.concurrent.Future

trait ProductsController extends BaseController with DateOfChangeHelper {
  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(edit: Boolean = false) = Authorised.async {
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


  def post(edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        Form2[Products](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(products(f, edit)))
          case ValidForm(_, data) => {
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              status <- statusService.getStatus
              _ <- dataCacheConnector.save[Hvd](Hvd.key,
                hvd.products(data)
              )
            } yield status match {
              case SubmissionDecisionApproved | ReadyForRenewal(_) if redirectToDateOfChange[Products](hvd.products, data) =>
                Redirect(routes.HvdDateOfChangeController.get())
              case _ => if (data.items.contains(Alcohol) | data.items.contains(Tobacco)) {
                Redirect(routes.ExciseGoodsController.get(edit))
              } else {
                edit match {
                  case true => Redirect(routes.SummaryController.get())
                  case false => Redirect(routes.HowWillYouSellGoodsController.get(edit))
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
  override val statusService = StatusService
}
