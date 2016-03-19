package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{AboutTheBusiness, ConfirmRegisteredOffice}
import models.businessmatching.{TypeOfBusiness, BusinessMatching}
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait TypeOfBusinessController extends BaseController {

  def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        dataCache.fetch[BusinessMatching](BusinessMatching.key) map {
        case Some(BusinessMatching(_, _, Some(data))) =>
          Ok(vat_registered(Form2[TypeOfBusiness](data), edit))
        case _ =>
          Ok(vat_registered(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[TypeOfBusiness](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(vat_registered(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCache.fetch[BusinessMatching](BusinessMatching.key)
            _ <- dataCache.save[BusinessMatching](BusinessMatching.key,
              businessMatching.typeOfBusiness(data)
            )
          } yield Redirect(routes.TypeOfBusinessController.get())
      }
    }
  }
}

object TypeOfBusinessController extends TypeOfBusinessController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
