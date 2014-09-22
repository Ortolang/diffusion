var ortolangDiffApp = angular.module('ortolangDiffApp', [
    'ngRoute',
    'ngAnimate',
    'ortolangDiffControllers',
    'ortolangDiffFilters',
    'angularFileUpload',
    'mgcrea.ngStrap.modal',
    'mgcrea.ngStrap.helpers.dimensions',
    'hljs'
    // 'mgcrea.ngStrap.helpers.debounce',
    // 'mgcrea.ngStrap.affix'
]);

ortolangDiffApp.config(['$routeProvider',
    function ($routeProvider) {
        $routeProvider.
            when('/workspaces/:wsName/:rootName/:elementPath*\/browse', {
                templateUrl: 'partials/browser.html'
            }).
            when('/workspaces/:wsName/:rootName/:elementPath*\/browseWU', {
                templateUrl: 'partials/workspaceElements.html',
                controller: 'WorkspaceElementsCtrl'
            }).
            when('/elements/:elementPath', {
                templateUrl: 'partials/detail.html',
                controller: 'DetailCtrl'
            }).
            when('/upload/:wsName', {
                templateUrl: 'partials/upload.html',
                controller: 'UploadCtrl'
            }).
            otherwise({
                redirectTo: '/workspaces/system/head///browse'
            });
    }]);

if (window.location.port === "63342") {
    // TODO: remove hack for local development
    ortolangDiffApp.urlBase = "http://localhost:8080/api";
} else {
    ortolangDiffApp.urlBase = "..";
}