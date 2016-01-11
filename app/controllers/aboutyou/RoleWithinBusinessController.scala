package controllers.aboutyou

import _root_.forms.{CompletedForm, InvalidForm, Form2, EmptyForm}
import config.AMLSAuthConnector
import config.AmlsPropertiesReader._
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import models._
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.CommonHelper

import scala.concurrent.Future

trait RoleWithinBusinessController extends FrontendController with Actions {

  val roles: Seq[(String, String)] =
    CommonHelper.mapSeqWithMessagesKey(
      getProperty("roleWithinBusiness").split(","),
      "aboutyou.rolewithinbusiness.lbl", Messages(_)
    )

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request => {
//      dataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](AboutYou.key) map {
//        case Some(data) => Ok(views.html.role_within_business(Form2[RoleWithinBusiness](data), roles, edit))
//        case _ => Ok(views.html.role_within_business(EmptyForm, roles, edit))
//      }
      Future.successful(Ok(""))
    }
  }

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
//      Form2[RoleWithinBusiness](request.body) match {
//        case f @ InvalidForm => Future.successful(BadRequest(views.html.role_within_business(f, roles, edit))),
//        case CompletedForm(_, role) => {
//          for {
//            aboutYou <- dataCacheConnector.fetchDataShortLivedCache[AboutYou](AboutYou.key)
//            _ <- dataCacheConnector.saveDataShortLivedCache[AboutYou](AboutYou.key,
//              aboutYou.roleWithinBusiness(role)
//            )
//          } yield Redirect(routes.SummaryController.get())
//        }
//      }
      Future.successful(Ok(""))
  }
}

object RoleWithinBusinessController extends RoleWithinBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
