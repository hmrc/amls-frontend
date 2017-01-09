package controllers.aboutthebusiness

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness._
import models.businessmatching.{BusinessType, BusinessMatching}
import models.businessmatching.BusinessType._
import play.api.mvc.Result
import utils.ControllerHelper
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait PreviouslyRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val form: Form2[PreviouslyRegistered] = (for {
            aboutTheBusiness <- response
            prevRegistered <- aboutTheBusiness.previouslyRegistered
          } yield Form2[PreviouslyRegistered](prevRegistered)).getOrElse(EmptyForm)
          Ok(previously_registered(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[PreviouslyRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(previously_registered(f, edit)))
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessType <- ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
              } yield {
                dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
                  getUpdatedModel(businessType, aboutTheBusiness, data))
                getRouting(businessType, edit, data)
              }).getOrElse(Redirect(routes.ConfirmRegisteredOfficeController.get(edit)))
          }
      }
    }
  }

  private def getUpdatedModel(businessType: BusinessType, aboutTheBusiness: AboutTheBusiness, data: PreviouslyRegistered): AboutTheBusiness = {
    data match {
      case PreviouslyRegisteredYes(_) => aboutTheBusiness.copy(previouslyRegistered = Some(data), activityStartDate = None,
                                                                hasChanged = true)
      case PreviouslyRegisteredNo => aboutTheBusiness.copy(previouslyRegistered = Some(data),
                                                                hasChanged = true)
    }
  }

  private def getRouting(businessType: BusinessType, edit: Boolean, data: PreviouslyRegistered): Result = {
    (businessType, edit, data) match {
      case (UnincorporatedBody | LPrLLP | LimitedCompany | Partnership, _, PreviouslyRegisteredYes(_)) =>
          Redirect (routes.VATRegisteredController.get (edit))
      case (_, _, PreviouslyRegisteredNo) =>
        Redirect (routes.ActivityStartDateController.get (edit))
      case (_, true, PreviouslyRegisteredYes(_)) => Redirect(routes.SummaryController.get())
      case (_, false, PreviouslyRegisteredYes(_)) =>
        Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
    }
  }
}

object PreviouslyRegisteredController extends PreviouslyRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
