package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople._
import jto.validation.{From, Rule, Write}
import jto.validation.forms._
import play.api.mvc.{AnyContent, Request, Result}
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.are_they_nominated_officer

import scala.concurrent.Future

object BooleanFormReadWrite {

  import jto.validation.forms.UrlFormEncoded
  import jto.validation.{From, Rule, Write}
  import utils.MappingUtils.Implicits._
  import jto.validation.forms.Rules._

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

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised {
      implicit authContext => implicit request =>

        Ok(are_they_nominated_officer(Form2[Option[Boolean]](None), edit, index, fromDeclaration))
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      import jto.validation.forms.Rules._
      implicit authContext => implicit request =>
        Form2[Boolean](request.body) match {
          case f: InvalidForm =>

            Future.successful(BadRequest(are_they_nominated_officer(f, edit, index, fromDeclaration)))

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
              redirectDependingOnEdit(index, edit, rpSeqOption, fromDeclaration)(request)
            }

          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }

  private def redirectDependingOnEdit(index: Int, edit: Boolean, rpSeqOption: Option[Seq[ResponsiblePeople]],
                                      fromDeclaration: Boolean = false)(implicit request: Request[AnyContent]) = {
    rpSeqOption match {
      case Some(rpSeq) => edit match {
        case true => Redirect(routes.DetailedAnswersController.get(index))
        case _ => Redirect(routes.VATRegisteredController.get(index, edit, fromDeclaration))
      }
      case _ => NotFound(notFoundView)
    }
  }
}

object AreTheyNominatedOfficerController extends AreTheyNominatedOfficerController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
