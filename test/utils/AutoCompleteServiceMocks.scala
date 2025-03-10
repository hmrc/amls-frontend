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

package utils

import models.autocomplete.NameValuePair
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.AutoCompleteService
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

trait AutoCompleteServiceMocks extends MockitoSugar {

  implicit val mockAutoComplete: AutoCompleteService = mock[AutoCompleteService]

  val nonUKCountries: Seq[SelectItem] = Seq(
    SelectItem(Some("AF"), "Afghanistan"),
    SelectItem(Some("AX"), "Åland Islands"),
    SelectItem(Some("AL"), "Albania"),
    SelectItem(Some("DZ"), "Algeria"),
    SelectItem(Some("AS"), "American Samoa")
  )

  when {
    mockAutoComplete.getCountries
  } thenReturn Some(
    Seq(
      NameValuePair("United Kingdom", "GB"),
      NameValuePair("Afghanistan", "AF"),
      NameValuePair("Åland Islands", "AX"),
      NameValuePair("Albania", "AL"),
      NameValuePair("Algeria", "DZ"),
      NameValuePair("American Samoa", "AS")
    )
  )

  when {
    mockAutoComplete.formOptions
  } thenReturn SelectItem(Some("GB"), "United Kingdom") +: nonUKCountries

  when {
    mockAutoComplete.formOptionsExcludeUK
  } thenReturn nonUKCountries
}
