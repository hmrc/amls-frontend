/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  def get(index: Int, edit: Boolean = false, fromDeclaration: Option[String] = None) =
    Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {rp =>
          Ok(are_they_nominated_officer(Form2[Option[Boolean]](None), edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
        }
    }


  def post(index: Int, edit: Boolean = false, fromDeclaration: Option[String] = None) =
    Authorised.async {
      import jto.validation.forms.Rules._
      implicit authContext => implicit request =>
        Form2[Boolean](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map { rp =>
              BadRequest(are_they_nominated_officer(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
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
                                      fromDeclaration: Option[String] = None)(implicit request: Request[AnyContent]) = {
    rpSeqOption match {
      case Some(rpSeq) => edit match {
        case true => Redirect(routes.DetailedAnswersController.get(index))
        case _ => Redirect(routes.SoleProprietorOfAnotherBusinessController.get(index, edit, fromDeclaration))
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
