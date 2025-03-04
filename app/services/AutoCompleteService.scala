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

package services

import javax.inject.Inject
import models.autocomplete.{CountryDataProvider, NameValuePair}
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

class AutoCompleteService @Inject() (data: CountryDataProvider) {
  lazy val getCountries: Option[Seq[NameValuePair]] = data.fetch map { s =>
    s.sortWith(_.name < _.name)
  }

  lazy val formOptions: Seq[SelectItem] = getCountries match {
    case Some(value) =>
      Seq(SelectItem()) ++
        value.map(pair => SelectItem(value = Some(pair.value), text = pair.name))
    case None        => throw new RuntimeException()
  }

  lazy val formOptionsExcludeUK: Seq[SelectItem] = getCountries match {
    case Some(value) =>
      Seq(SelectItem()) ++
        value
          .filterNot(_.value == "GB")
          .map(pair => SelectItem(value = Some(pair.value), text = pair.name))
    case None        => throw new RuntimeException()
  }
}
