package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.time_at_address

import scala.concurrent.Future

trait TimeAtAdditionalAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName), _, _, Some(ResponsiblePersonAddressHistory(_, Some(ResponsiblePersonAddress(_, Some(additionalAddress))), _)), _, _, _, _, _, _, _, _, _, _)) =>
            Ok(time_at_address(Form2[TimeAtAddress](additionalAddress), false, edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
            Ok(time_at_address(Form2(DefaultAddressHistory), false, edit, index, fromDeclaration, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
        (Form2[TimeAtAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map { rp =>
              BadRequest(time_at_address(f, false, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            getData[ResponsiblePeople](index) flatMap { responsiblePerson =>
              (for {
                rp <- responsiblePerson
                addressHistory <- rp.addressHistory
                additionalAddress <- addressHistory.additionalAddress
              } yield {
                val additionalAddressWithTime = additionalAddress.copy(
                  timeAtAddress = Some(data)
                )
                doUpdate(index, additionalAddressWithTime).map { _ =>
                  redirectTo(index, edit, fromDeclaration, data)
                }
              }) getOrElse Future.successful(NotFound(notFoundView))
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def redirectTo(index: Int, edit: Boolean, fromDeclaration: Boolean, data: TimeAtAddress) = {
    (data, edit) match {
      case (ThreeYearsPlus, false) => Redirect(routes.PositionWithinBusinessController.get(index, edit, fromDeclaration))
      case (OneToThreeYears, false) => Redirect(routes.PositionWithinBusinessController.get(index, edit, fromDeclaration))
      case (_, false) => Redirect(routes.AdditionalExtraAddressController.get(index, edit, fromDeclaration))
      case (ThreeYearsPlus, true) => Redirect(routes.DetailedAnswersController.get(index))
      case (OneToThreeYears, true) => Redirect(routes.DetailedAnswersController.get(index))
      case (_, true) => Redirect(routes.AdditionalExtraAddressController.get(index, edit, fromDeclaration))
    }
  }

  private def doUpdate(index: Int, data: ResponsiblePersonAddress)(implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) { res =>
      res.addressHistory(
        (res.addressHistory, data.timeAtAddress) match {
          case (Some(a), Some(ThreeYearsPlus)) => a.additionalAddress(data).removeAdditionalExtraAddress
          case (Some(a), Some(OneToThreeYears)) => a.additionalAddress(data).removeAdditionalExtraAddress
          case (Some(a), _) => a.additionalAddress(data)
          case _ => ResponsiblePersonAddressHistory(additionalAddress = Some(data))
        })
    }
  }
}

object TimeAtAdditionalAddressController extends TimeAtAdditionalAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
