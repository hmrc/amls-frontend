package controllers.asp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.asp.{OtherBusinessTaxMatters, Asp}
import views.html.asp._
import scala.concurrent.Future

trait OtherBusinessTaxMattersController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Asp](Asp.key) map {
        response =>
          val form: Form2[OtherBusinessTaxMatters] = (for {
            asp <- response
            otherTax <- asp.otherBusinessTaxMatters
          } yield Form2[OtherBusinessTaxMatters](otherTax)).getOrElse(EmptyForm)
          Ok(other_business_tax_matters(form, edit))
      }
  }

}

object OtherBusinessTaxMattersController extends OtherBusinessTaxMattersController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}