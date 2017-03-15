package controllers.responsiblepeople

import javax.inject.Inject

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.responsiblepeople.ResponsiblePeople
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, ControllerHelper}

class SoleProprietorOfAnotherBusinessController @Inject()(
                                                           val dataCacheConnector: DataCacheConnector,
                                                           val authConnector: AuthConnector) extends RepeatingSection with BaseController{

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {rp =>
          Ok(views.html.responsiblepeople.sole_proprietor(EmptyForm, true, 0, true, ""))
        }
    }

}