'use strict';

/* Services */

var ortolangDiffServices = angular.module(
    'ortolangDiffServices',
    ['ngResource']
);

ortolangDiffServices.factory('Profil', ['$resource',
    function($resource){
        return $resource(ortolangDiffApp.urlBase + '/rest/profiles/:userId/', {userId:'@id'} ,{
            query: {
                method:'GET',
                isArray:false
            }
        });
    }
]);

ortolangDiffServices.factory('Workspaces', ['$resource',
    function($resource){
        return $resource(ortolangDiffApp.urlBase + '/rest/workspaces/', {} ,{
            query: {
                method:'GET'
            }
        });
    }
]);
