package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness._
import models.businessmatching.{BusinessMatching, BusinessType}
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership, UnincorporatedBody}
import play.api.mvc.Result
import utils.ControllerHelper
import views.html.aboutthebusiness.activity_start_date

import scala.concurrent.Future

trait ActivityStartDateController extends BaseController {
  def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val form: Form2[ActivityStartDate] = (for {
            aboutTheBusiness <- response
            activityStartDate <- aboutTheBusiness.activityStartDate
          } yield Form2[ActivityStartDate](activityStartDate)).getOrElse(EmptyForm)
          Ok(activity_start_date(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ActivityStartDate](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(activity_start_date(f, edit)))
        case ValidForm(_, data) =>
          dataCache.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessType <- ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
              } yield {
                dataCache.save[AboutTheBusiness](AboutTheBusiness.key, aboutTheBusiness.activityStartDate(data))
                getRouting(businessType, edit)
              }).getOrElse(Redirect(routes.ConfirmRegisteredOfficeController.get(edit)))

          }
      }
  }

  private def getRouting(businessType: BusinessType, edit: Boolean): Result = {
    (businessType, edit) match {
      case (UnincorporatedBody | LPrLLP | LimitedCompany | Partnership, _) =>
        Redirect(routes.VATRegisteredController.get(edit))
      case (_, true) => Redirect(routes.SummaryController.get())
      case (_, false) =>
        Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
    }
  }
}

object ActivityStartDateController extends ActivityStartDateController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
