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

package utils.tcsp

import models.tcsp.ProvidedServices.Other
import models.tcsp.TcspTypes.RegisteredOfficeEtc
import models.tcsp._
import play.api.Logging
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key

import javax.inject.Inject

class CheckYourAnswersHelper @Inject()() extends Logging {

  def getSummaryList(model: Tcsp)(implicit messages: Messages): SummaryList = {

    SummaryList(
      Seq(kindOfServiceProviderRow(sortProviders(model))) ++
      Seq(
        onlyOffTheShelfCompsSoldRow(model),
        complexCorpStructureCreationRow(model),
        providedServicesRow(model),
        doesServicesOfAnotherTCSPRow(model)
      ).flatten ++
      anotherTCSPSupervisionRows(model).getOrElse(Seq.empty)
    )
  }

  private def sortProviders(data: Tcsp)(implicit messages: Messages): List[String] = {

    val sortedList = (for {
      types <- data.tcspTypes
      providers <- Some(types.serviceProviders)
      labels <- Some(providers.collect {
        case provider if !provider.value.eq("05") => messages(s"tcsp.service.provider.lbl.${provider.value}")
      }
      )
      specialCase <- Some(providers.collect {
        case provider if provider.value.eq("05") => messages(s"tcsp.service.provider.lbl.05")
      }
      )
    } yield labels.toList.sorted ++ specialCase.toList).getOrElse(List())


    if (sortedList.isEmpty) {
      logger.warn(s"[tcsp][CheckYourAnswersHelper][sortProviders] - tcsp provider list is empty")
    }

    sortedList
  }

  private def kindOfServiceProviderRow(sortedProviders: List[String])(implicit messages: Messages): SummaryListRow = {

    val answer = if(sortedProviders.length > 1) {
      toBulletList(sortedProviders)
    } else {
      Value(Text(sortedProviders.mkString))
    }
    SummaryListRow(
      Key(Text(messages("tcsp.kind.of.service.provider.title"))),
      answer,
      actions = editAction(
        controllers.tcsp.routes.TcspTypesController.get(true).url,
        "tcsp.checkYourAnswers.change.tcspType",
        "tcspkindserviceprovider-edit"
      )
    )
  }

  private def onlyOffTheShelfCompsSoldRow(model: Tcsp)(implicit messages: Messages): Option[SummaryListRow] = {

    def label(answer: OnlyOffTheShelfCompsSold): String = answer match {
      case OnlyOffTheShelfCompsSoldYes => booleanToLabel(bool = true)
      case OnlyOffTheShelfCompsSoldNo => booleanToLabel(bool = false)
    }

    model.onlyOffTheShelfCompsSold.map { s =>
      row(
        "tcsp.off-the-shelf.companies.lbl",
        label(s),
        editAction(
          controllers.tcsp.routes.OnlyOffTheShelfCompsSoldController.get(true).url,
          "tcsp.checkYourAnswers.change.otsCompanies",
          "onlyOffTheShelfCompsSold-edit"
        )
      )
    }
  }

  private def complexCorpStructureCreationRow(model: Tcsp)(implicit messages: Messages): Option[SummaryListRow] = {

    def label(answer: ComplexCorpStructureCreation): String = answer match {
      case ComplexCorpStructureCreationYes => booleanToLabel(bool = true)
      case ComplexCorpStructureCreationNo => booleanToLabel(bool = false)
    }

    model.complexCorpStructureCreation.map { s =>
      row(
        "tcsp.create.complex.corporate.structures.lbl",
        label(s),
        editAction(
          controllers.tcsp.routes.ComplexCorpStructureCreationController.get(true).url,
          "tcsp.checkYourAnswers.change.complexCompStructs",
          "complexCorpStructureCreation-edit"
        )
      )
    }
  }

  private def providedServicesRow(model: Tcsp)(implicit messages: Messages): Option[SummaryListRow] = {

    def label(answer: ProvidedServices): Value = answer.services.toSeq match {
      case Seq(x) => Value(Text(x.getMessage))
      case seq =>
        toBulletList(
          (seq.filterNot(_.isInstanceOf[Other]) ++ seq.find(_.isInstanceOf[Other])).map(_.getMessage)
        )
    }

    for {
      types <- model.tcspTypes
      services <- model.providedServices
    } yield {

      if (types.serviceProviders.contains(RegisteredOfficeEtc)) {
        Some(
          SummaryListRow(
            Key(Text(messages("tcsp.provided_services.title"))),
            label(services),
            actions = editAction(
              controllers.tcsp.routes.ProvidedServicesController.get(true).url,
              "tcsp.checkYourAnswers.change.servicesProvided",
              "tcsptypes-edit"
            )
          )
        )
      } else {
        None
      }
    }
  }.flatten

  private def doesServicesOfAnotherTCSPRow(model: Tcsp)(implicit messages: Messages): Option[SummaryListRow] = {

    model.doesServicesOfAnotherTCSP.map { x =>
      row(
        "tcsp.servicesOfAnotherTcsp.title",
        booleanToLabel(x),
        editAction(
          controllers.tcsp.routes.ServicesOfAnotherTCSPController.get(true).url,
          "tcsp.checkYourAnswers.change.whereServicesProvided",
          "servicesofanothertcsp-edit"
        )
      )
    }
  }

  private def anotherTCSPSupervisionRows(model: Tcsp)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def yesNoRow(boolean: Boolean): SummaryListRow = row(
      "tcsp.anothertcspsupervision.title",
      booleanToLabel(boolean),
      editAction(
        controllers.tcsp.routes.AnotherTCSPSupervisionController.get(true).url,
        "tcsp.checkYourAnswers.change.otherBusAtAddressSupervised",
        "anothertcsp-edit"
      )
    )

    model.servicesOfAnotherTCSP.map {
      case ServicesOfAnotherTCSPYes(Some(mlrRefNumber)) => Seq(
        yesNoRow(true),
        row(
          "tcsp.anothertcspsupervision.cya.additional.header",
          mlrRefNumber,
          editAction(
            controllers.tcsp.routes.AnotherTCSPSupervisionController.get(true).url,
            "tcsp.checkYourAnswers.change.tcspMLRNo",
            "mlrrefnumber-edit"
          )
        )
      )
      case ServicesOfAnotherTCSPYes(None) => Seq(yesNoRow(true))
      case ServicesOfAnotherTCSPNo => Seq(yesNoRow(false))
    }
  }

  private def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
    messages("lbl.yes")
  } else {
    messages("lbl.no")
  }

  private def toBulletList[A](coll: Seq[A]): Value = Value(
    HtmlContent(
      Html(
        "<ul class=\"govuk-list govuk-list--bullet\">" +
          coll.map { x =>
            s"<li>$x</li>"
          }.mkString +
          "</ul>"
      )
    )
  )

  private def row(title: String, label: String, actions: Option[Actions])(implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      Key(Text(messages(title))),
      Value(Text(label)),
      actions = actions
    )
  }

  private def editAction(route: String, hiddenText: String, id: String)(implicit messages: Messages): Option[Actions] = {
    Some(Actions(
      items = Seq(ActionItem(
        route,
        Text(messages("button.edit")),
        visuallyHiddenText = Some(messages(hiddenText)),
        attributes = Map("id" -> id)
      ))
    ))
  }
}
