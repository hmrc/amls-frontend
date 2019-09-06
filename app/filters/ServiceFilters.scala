package filters

import com.google.inject.Inject
import config.WhitelistFilter
import play.api.http.DefaultHttpFilters
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

class ServiceFilters @Inject()(defaultFilters: FrontendFilters, whitelistFilter: WhitelistFilter)
  extends DefaultHttpFilters({
    defaultFilters.filters :+ whitelistFilter
  }:_*)
