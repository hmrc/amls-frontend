/*
 * Copyright 2024 HM Revenue & Customs
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

package utils.businessmatching

import config.ApplicationConfig
import models.CheckYourAnswersField
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.businessmatching._
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryList}
import views.html.components.{Button => SubmissionButton}

import javax.inject.Inject

class CheckYourAnswersHelper @Inject() (button: SubmissionButton, appConfig: ApplicationConfig) {

  def createSummaryList(businessMatching: BusinessMatching, isPreSubmission: Boolean, isPending: Boolean)(implicit
    messages: Messages,
    request: Request[_]
  ): SummaryList =
    SummaryList(
      Seq(
        businessAddress(businessMatching),
        registrationType(businessMatching, isPreSubmission),
        registeredServices(businessMatching, isPreSubmission, isPending),
        moneyServiceBusinessActivities(businessMatching, isPending)
      ).flatten ++
        psrRegistrationNumber(businessMatching).getOrElse(Seq.empty[SummaryListRow])
    )

  def getSubmitButton(
    appliedForPSRNumberOpt: Option[BusinessAppliedForPSRNumber],
    isPreSubmission: Boolean,
    preAppCompleted: Boolean
  )(implicit messages: Messages): Option[Html] =
    if (isPreSubmission) {
      appliedForPSRNumberOpt map {
        case BusinessAppliedForPSRNumberYes(psrNumber) =>
          if (preAppCompleted) {
            button("businessmatching.summary.noedit.anchortext")
          } else {
            button("businessmatching.button.confirm.start")
          }
        case BusinessAppliedForPSRNumberNo             =>
          button(
            "button.logout",
            "logout",
            Some(appConfig.logoutUrl)
          )
      }
    } else {
      Some(button("businessmatching.summary.noedit.anchortext"))
    }

  private def businessAddress(businessMatching: BusinessMatching)(implicit messages: Messages): Option[SummaryListRow] =
    businessMatching.reviewDetails.map { review =>
      SummaryListRow(
        Key(
          Text(messages("businessmatching.summary.business.address.lbl"))
        ),
        Value(
          HtmlContent(
            Html(
              "<ul class=\"govuk-list\">" +
                review.businessAddress.toLines.map { line =>
                  s"""<li>$line<li>"""
                }.mkString
                + "</ul>"
            )
          )
        )
      )
    }

  private def registrationType(businessMatching: BusinessMatching, isPreSubmission: Boolean)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {
    for {
      review       <- businessMatching.reviewDetails
      businessType <- review.businessType
    } yield businessType match {
      case BusinessType.LPrLLP | BusinessType.LimitedCompany =>
        businessMatching.companyRegistrationNumber map { regNumber =>
          SummaryListRow(
            Key(Text(messages("businessmatching.registrationnumber.title"))),
            Value(Text(regNumber.companyRegistrationNumber), "registration-number"),
            actions = Some(
              Actions(
                items = Seq(
                  ActionItem(
                    controllers.businessmatching.routes.CompanyRegistrationNumberController.get(true).url,
                    Text(messages("button.edit")),
                    visuallyHiddenText = Some(messages("businessmatching.checkYourAnswers.change.CompanyReg")),
                    attributes = Map("id" -> "edit-registration-number")
                  )
                )
              )
            )
          )
        }
      case BusinessType.UnincorporatedBody                   =>
        businessMatching.typeOfBusiness map { businessType =>
          SummaryListRow(
            Key(Text(messages("businessmatching.typeofbusiness.title"))),
            Value(Text(businessType.typeOfBusiness), "type-of-business"),
            actions = if (isPreSubmission) {
              Some(
                Actions(
                  items = Seq(
                    ActionItem(
                      href = controllers.businessmatching.routes.TypeOfBusinessController.get(true).url,
                      content = Text(messages("button.edit")),
                      visuallyHiddenText = Some(messages("businessmatching.checkYourAnswers.change.CompanyReg"))
                    )
                  )
                )
              )
            } else {
              None
            }
          )
        }
      case _                                                 => None
    }
  }.flatten

  private def registeredServices(businessMatching: BusinessMatching, isPreSubmission: Boolean, isPending: Boolean)(
    implicit messages: Messages
  ): Option[SummaryListRow] = {

    val serviceValues = businessMatching.activities map { x =>
      getMultipleAnswersHtml[BusinessActivity](
        x.businessActivities.toSeq.sortBy(_.toString),
        "businessmatching.registerservices.servicename.lbl"
      )
    }

    serviceValues map {
      case Some(values) =>
        Some(
          SummaryListRow(
            key = Key(Text(messages("businessmatching.registerservices.title"))),
            value = Value(values),
            actions = if (!isPending) {
              Some(
                Actions(items =
                  Seq(
                    ActionItem(
                      href = if (isPreSubmission) {
                        controllers.businessmatching.routes.RegisterServicesController.get().url
                      } else {
                        controllers.businessmatching.updateservice.routes.ChangeBusinessTypesController.get().url
                      },
                      content = Text(messages("button.edit")),
                      visuallyHiddenText = Some(messages("businessmatching.checkYourAnswers.change.ServicesRegstd")),
                      attributes = Map("id" -> "businessactivities-edit")
                    )
                  )
                )
              )
            } else {
              None
            }
          )
        )
      case _            => None
    }
  }.flatten

  private def moneyServiceBusinessActivities(businessMatching: BusinessMatching, isPending: Boolean)(implicit
    messages: Messages
  ): Option[SummaryListRow] = {

    val msbValues = businessMatching.msbServices map { x =>
      getMultipleAnswersHtml[BusinessMatchingMsbService](
        x.msbServices.toSeq.sortBy(_.toString),
        "businessmatching.services.list.lbl"
      )
    }

    msbValues map {
      case Some(values) =>
        Some(
          SummaryListRow(
            key = Key(Text(messages("businessmatching.services.title"))),
            value = Value(values),
            actions = if (!isPending) {
              Some(
                Actions(items =
                  Seq(
                    ActionItem(
                      href = controllers.businessmatching.routes.MsbSubSectorsController.get(true).url,
                      content = Text(messages("button.edit")),
                      visuallyHiddenText = Some(messages("businessmatching.checkYourAnswers.change.MSBActivities")),
                      attributes = Map("id" -> "msbservices-edit")
                    )
                  )
                )
              )
            } else {
              None
            }
          )
        )
      case _            => None
    }

  }.flatten

  private def psrRegistrationNumber(
    businessMatching: BusinessMatching
  )(implicit messages: Messages, request: Request[_]): Option[Seq[SummaryListRow]] = {
    for {
      businessMatchingMsbServices <- businessMatching.msbServices
      isTransmittingMoney          = businessMatchingMsbServices.msbServices.contains(TransmittingMoney)
      appliedForPSRNumber         <- businessMatching.businessAppliedForPSRNumber
    } yield
      if (isTransmittingMoney) {
        appliedForPSRNumber match {
          case BusinessAppliedForPSRNumberYes(_) =>
            Some(
              Seq(
                SummaryListRow(
                  Key(Text(messages("businessmatching.psr.number.title"))),
                  Value(Text(messages("lbl.yes")))
                ),
                SummaryListRow(
                  Key(Text(messages("businessmatching.psr.number.cya.title"))),
                  Value(Text(request.session.get("originalPsrNumber").getOrElse(""))),
                  actions = Some(
                    Actions(
                      items = Seq(
                        ActionItem(
                          href = controllers.businessmatching.routes.PSRNumberController.get(true).url,
                          content = Text(messages("button.edit")),
                          visuallyHiddenText = Some(messages("businessmatching.checkYourAnswers.change.PSRReg")),
                          attributes = Map("id" -> "edit-psr-number")
                        )
                      )
                    )
                  )
                )
              )
            )
          case _                                 => None
        }
      } else {
        None
      }
  }.flatten

  private def getMultipleAnswersHtml[A <: CheckYourAnswersField](values: Seq[A], msgPrefix: String)(implicit
    messages: Messages
  ) =
    values match {
      case answers if answers.size > 1  =>
        Some(
          HtmlContent(
            "<ul class=\"govuk-list govuk-list--bullet\">" +
              answers.map { x =>
                s"<li>${messages(s"$msgPrefix.${x.value}")}</li>"
              }.mkString +
              "</ul>"
          )
        )
      case answers if answers.size == 1 => Some(Text(messages(s"$msgPrefix.${answers.head.value}")))
      case _                            => None
    }
}
