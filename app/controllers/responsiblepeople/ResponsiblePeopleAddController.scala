package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

trait ResponsiblePeopleAddController extends BaseController with RepeatingSection {
  def get(displayGuidance: Boolean = true, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      addData[ResponsiblePeople](ResponsiblePeople.default(None)).map {idx =>
        Redirect {
          displayGuidance match {
            case true => controllers.responsiblepeople.routes.WhoMustRegisterController.get(idx, fromDeclaration)
            case false => controllers.responsiblepeople.routes.PersonNameController.get(idx)
          }
        }
      }
    }
  }
}

object ResponsiblePeopleAddController extends ResponsiblePeopleAddController {
  // $COVERAGE-OFF$
  override def dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
