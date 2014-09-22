var ortolangDiffApp = angular.module('ortolangDiffApp', [
    'ngRoute',
    'ortolangDiffControllers',
    'ortolangDiffServices'

//    'ngAnimate',
//    'ortolangDiffFilters',
//    'angularFileUpload',
//    'mgcrea.ngStrap.modal',
//    'mgcrea.ngStrap.helpers.dimensions'
    // 'mgcrea.ngStrap.helpers.debounce',
    // 'mgcrea.ngStrap.affix'
]);

ortolangDiffApp.config(['$routeProvider',
    function ($routeProvider) {
        $routeProvider.
//            when('/workspaces/:wsName/:rootName/:elementPath*\/browse', {
//                templateUrl: 'partials/browser.html'
//            }).
//            when('/workspaces/:wsName/:rootName/:elementPath*\/browseWU', {
//                templateUrl: 'partials/workspaceElements.html',
//                controller: 'WorkspaceElementsCtrl'
//            }).
//            when('/elements/:elementPath', {
//                templateUrl: 'partials/detail.html',
//                controller: 'DetailCtrl'
//            }).
            when('/project/:wsName', {
                templateUrl: 'partials/workspace.html',
                controller: 'wkCtrl'
            }).
            when('/portal', {
                templateUrl: 'partials/search.html',
                controller: 'searchCtrl',
                controllerAs: 'search'
            }).
            when('/login', {
                templateUrl: 'partials/login.html',
                controller: 'loginCtrl',
                controllerAs: 'login'
            }).
            otherwise({
                redirectTo: '/portal'
            });

    }]);

if (window.location.port === "63342") {
    // TODO: remove hack for local development
    ortolangDiffApp.urlBase = "http://localhost:8080/api";
} else {
    ortolangDiffApp.urlBase = "..";
}

