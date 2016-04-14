package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.responsiblepeople.TimeAtAddress.{Empty, ZeroToFiveMonths, ThreeYearsPlus}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.RepeatingSection
import views.html.responsiblepeople.additional_address

import scala.concurrent.Future

trait AdditionalAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), Empty)

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            response =>
              val form: Form2[ResponsiblePersonAddress] = (for {
                responsiblePeople <- response
                addressHistory <- responsiblePeople.addressHistory
                additionalAddress <- addressHistory.additionalAddress
              } yield Form2[ResponsiblePersonAddress](additionalAddress)).getOrElse(Form2(DefaultAddressHistory))
              Ok(additional_address(form, edit, index))
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {
          Form2[ResponsiblePersonAddress](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.responsiblepeople.additional_address(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- doUpdate(index, data)
              } yield (data.timeAtAddress, edit) match {
                case (ThreeYearsPlus, false) => Redirect(routes.AdditionalAddressController.get(index, edit)) //TODO: Business Position
                case (_, false) => Redirect(routes.AdditionalExtraAddressController.get(index, edit))
                case (_, true) => Redirect(routes.SummaryController.get())
              }
          }
        }
      }
    }

  private def doUpdate(index: Int, data: ResponsiblePersonAddress)(implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateData[ResponsiblePeople](index) {
      case Some(res) => {
        Some(res.addressHistory(
          res.addressHistory match {
            case Some(a) => a.additionalAddress(data)
            case _ => ResponsiblePersonAddressHistory(additionalAddress = Some(data))
          })
        )
      }
      case _ =>
        Some(ResponsiblePeople(
          addressHistory = Some(ResponsiblePersonAddressHistory(
            additionalAddress = Some(data)))))
    }
  }

}

object AdditionalAddressController extends AdditionalAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
