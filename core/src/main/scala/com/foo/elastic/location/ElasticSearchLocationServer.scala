package com.foo.elastic.location

import com.foo.elastic.ElasticSearchBaseServer
import com.foo.location.LocationServer

trait ElasticSearchLocationServer extends LocationServer with ElasticSearchLocationReadServer with ElasticSearchLocationWriteServer {
  self : ElasticSearchBaseServer =>
}
