<%@ page language="java" contentType="text/html; charset=UTF-8" session="false" pageEncoding="UTF-8" %>
<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --%>
<!doctype html>
<html>
<head>
  <title>Hippo Essentials</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main.css?v=${project.version}"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/components/angular-ui-tree/dist/angular-ui-tree.min.css?v=${project.version}"/>
  <script type="application/javascript">
    window.SERVER_URL = '<%=request.getServerName()+':'+request.getServerPort()%>';
  </script>
  <script src="${pageContext.request.contextPath}/components/jquery/jquery.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular/angular.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/angular/angular-sanitize.min.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/chosen/chosen.jquery.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/underscore/underscore.js?v=${project.version}"></script>

  <script src="${pageContext.request.contextPath}/components/bootstrap/dist/js/bootstrap.js?v=${project.version}"></script>


  <%--  NOTE: enable once R&D team upgrades version(s)--%>
  <%--

    <script src="${pageContext.request.contextPath}/components/angular-bootstrap/ui-bootstrap.min.js"></script>
    <script src="${pageContext.request.contextPath}/components/angular-bootstrap/ui-bootstrap-tpls.min.js"></script>
  --%>
  <script src="${pageContext.request.contextPath}/js/lib/ui-bootstrap-tpls.min.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-route.min.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-ui-router.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/lib/angular-animate.js?v=${project.version}"></script>

  <%-- HIPPO THEME DEPS --%>
  <script src="${pageContext.request.contextPath}/components/angular-ui-tree/dist/angular-ui-tree.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/hippo-plugins/dist/js/main.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/components/hippo-theme/dist/js/main.js?v=${project.version}"></script>

  <%-- ESSENTIALS --%>
  <script src="${pageContext.request.contextPath}/js/Essentials.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/app.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/routes.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/directives.js?v=${project.version}"></script>
  <script src="${pageContext.request.contextPath}/js/controllers.js?v=${project.version}"></script>

  <link rel="icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/images/favicon.ico" type="image/x-icon"/>
</head>
<body id="container"  ng-cloak ng-class="{'log-visible':feedbackMessages.length && addLogClass}">
<essentials-notifier ng-show="feedbackMessages.length" messages="feedbackMessages"></essentials-notifier>


<div class="hippo-navbar navbar navbar-default navbar-fixed-top" ng-controller="navbarCtrl" ng-hide="INTRODUCTION_DISPLAYED">
  <div class="container-fluid">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <span class="badge badge-primary notification-badge">{{TOTAL_NEEDS_ATTENTION}}</span>
      <a class="navbar-brand" href="#">Hippo Essentials {{}}</a>
      <p class="navbar-text navbar-title">
        {{getPageTitle()}}
      </p>
      <div class="navbar-text navbar-icons">
        <a href="#/build" class="navbar-link">
          <span class="fa fa-refresh"></span> <span class="hidden-xs">Rebuild</span>
        </a>
        <a href="#/build" class="navbar-link">
          <span ng-show="NEEDS_REBUILD" class="fa fa-bell-o fa-danger"></span>
        </a>
        <a ng-click="showMessages()" ng-show="feedbackMessages.length && showMessagesNavbarLink">
          <span class="fa fa-info-circle"></span>
          <span class="badge pull-right alert-info">{{feedbackMessages.length}}</span>
        </a>
      </div>
    </div>

    <div class="navbar-collapse collapse ng-scope" ng-controller="mainMenuCtrl" ng-hide="INTRODUCTION_DISPLAYED">
      <ul class="nav navbar-nav" ng-hide="INTRODUCTION_DISPLAYED">
        <li ng-class="{true:'active', false:''}[isPageSelected('#/library')]">
          <a href="#/library">
            <i class="fa fa-shopping-cart fa-2x fa-fw fa-middle"></i>
            <span>Library</span>
          </a>
        </li>
        <li ng-show="INSTALLED_FEATURES > 0" ng-class="{true:'active', false:''}[isPageSelected('#/installed-features')]">
          <a href="#/installed-features">
            <i class="fa fa-dropbox fa-2x fa-fw fa-middle"></i>
            <span>Installed features</span>
            <span ng-show="TOTAL_NEEDS_ATTENTION > 0" class="badge pull-right alert-danger">{{TOTAL_NEEDS_ATTENTION}}</span>
          </a>
        </li>
        <li ng-class="{true:'active', false:''}[isPageSelected('#/tools')]">
          <a href="#/tools">
            <i class="fa fa-wrench fa-2x fa-fw fa-middle"></i>
            <span>Tools</span>
          </a>
        </li>
        <li>
          <a target="_blank" href="https://issues.onehippo.com/rest/collectors/1.0/template/form/a23eddf8?os_authType=none">
            <i class="fa fa-pencil fa-2x fa-fw fa-middle"></i>
            <span>Feedback</span></a>
        </li>
      </ul>
    </div>
  </div>
</div>



<div class="container-fluid container-has-hippo-navbar">
  <div class="row">
    <div class="col-lg-12" ui-view autoscroll="false">
    </div>
  </div>
  <div class="row">
    <div class="col-lg-12">
      <p class="text-center" id="footer">
        <em>version: ${project.version}</em>
      </p>
    </div>
  </div>
</div>
<!-- Include the loader.js script -->
<script src="${pageContext.request.contextPath}/js/loader.js" data-modules="http://<%=request.getServerName()+':'+request.getServerPort()%>/essentials/rest/plugins/modules"></script>

</body>
</html>