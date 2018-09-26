package com.foo.elastic.product

import com.foo.elastic.ElasticSearchBaseServer
import com.foo.product.ProductServer

trait ElasticSearchProductServer extends ProductServer with ElasticSearchReadServer with ElasticSearchWriteServer {
  self : ElasticSearchBaseServer =>
}
