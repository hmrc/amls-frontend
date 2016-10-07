package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople._
import play.api.data.mapping.{From, Rule, Write}
import play.api.data.mapping.forms._
import play.api.mvc.{Request, Result}
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.are_they_nominated_officer

import scala.concurrent.Future

object BooleanFormReadWrite {

  import play.api.data.mapping.forms.UrlFormEncoded
  import play.api.data.mapping.{From, Rule, Write}
  import utils.MappingUtils.Implicits._
  import play.api.data.mapping.forms.Rules._

  def formWrites(fieldName: String): Write[Option[Boolean], UrlFormEncoded] = Write { data: Option[Boolean] => Map(fieldName -> Seq(data.toString)) }

  def formRule(fieldName: String): Rule[UrlFormEncoded, Boolean] = From[UrlFormEncoded] { __ =>
    (__ \ fieldName).read[Boolean].withMessage("error.required.rp.nominated_officer")
  }
}

trait AreTheyNominatedOfficerController extends RepeatingSection with BaseController {


  val dataCacheConnector: DataCacheConnector
  val FIELDNAME = "isNominatedOfficer"
  implicit val boolWrite = BooleanFormReadWrite.formWrites(FIELDNAME)
  implicit val boolRead = BooleanFormReadWrite.formRule(FIELDNAME)

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised {
        implicit authContext => implicit request =>

          Ok(are_they_nominated_officer(Form2[Option[Boolean]](None), edit, index))
      }
    }


  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        import play.api.data.mapping.forms.Rules._
        implicit authContext => implicit request =>
          Form2[Boolean](request.body) match {
            case f: InvalidForm =>

              Future.successful(BadRequest(are_they_nominated_officer(f, edit, index)))

            case ValidForm(_, data) => {

              for {
                _ <- updateDataStrict[ResponsiblePeople](index) { rp =>

                  rp.positions match {
                    case Some(pos) if (data & !rp.isNominatedOfficer) =>
                      rp.positions(pos.copy(pos.positions + NominatedOfficer, pos.startDate))
                    case _ => rp
                  }
                }
                rpSeqOption <- dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
              } yield {
                rpSeqOption match {
                  case Some(rpSeq) => personalTaxRouter(index, edit, rpSeq)
                  case _ => NotFound(notFoundView)
                }
              }

            }.recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
      }
    }

  private def personalTaxRouter(index: Int, edit: Boolean, rpSeq: Seq[ResponsiblePeople])(implicit request:Request[_]): Result = {
    rpSeq.lift(index - 1) match {
      case Some(x) => (isPersonalTax(x), edit) match {
        case (false, false) => Redirect(routes.ExperienceTrainingController.get(index))
        case (false, true) => Redirect(routes.DetailedAnswersController.get(index))
        case _ => Redirect(routes.VATRegisteredController.get(index, edit))

      }
      case _ => NotFound(notFoundView)
    }
  }

  private def isPersonalTax(responsiblePeople: ResponsiblePeople) = {
    responsiblePeople.positions match {
      case Some(p) => p.personalTax
      case _ => false
    }
  }

}

object AreTheyNominatedOfficerController extends AreTheyNominatedOfficerController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
