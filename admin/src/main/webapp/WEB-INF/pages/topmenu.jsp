<nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
	<div class="container-fluid">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#sidebar">
				<span class="sr-only">Changer le mode navigation</span> <span class="icon-bar"></span> <span class="icon-bar"></span> <span class="icon-bar"></span>
			</button>
			<a id="menu-medium" class="sidebar-toggle tooltips"> <i class="fa fa-outdent"></i> </a> 
			<a class="navbar-brand" href="home.sdo"> <img src="img/logo.png" alt="logo" width="79" height="26"></a>
		</div>
		<div class="navbar-center">Tableau de bord</div>
		<div class="navbar-collapse collapse">
			<ul class="nav navbar-nav pull-right header-menu">
				<li class="dropdown" id="notifications-header">
					<a href="#" class="dropdown-toggle" data-toggle="dropdown" data-hover="dropdown" data-close-others="true"> 
						<i class="glyph-icon flaticon-notifications"></i> <span class="badge badge-danger badge-header">6</span>
					</a>
					<ul class="dropdown-menu">
						<li class="dropdown-header clearfix">
							<p class="pull-left">Notifications</p>
						</li>
						<li>
							<ul class="dropdown-menu-list withScroll" data-height="220">
								<li><a href="#"> <i	class="fa fa-star p-r-10 f-18 c-orange"></i> Steve have rated your photo <span class="dropdown-time">Just now</span></a></li>
								<li><a href="#"> <i	class="fa fa-heart p-r-10 f-18 c-red"></i> John added you to his favs <span class="dropdown-time">15 mins</span></a></li>
								<li><a href="#"> <i class="fa fa-file-text p-r-10 f-18"></i> New document available <span class="dropdown-time">22 mins</span></a></li>
								<li><a href="#"> <i	class="fa fa-picture-o p-r-10 f-18 c-blue"></i> New picture added <span class="dropdown-time">40 mins</span></a></li>
								<li><a href="#"> <i	class="fa fa-bell p-r-10 f-18 c-orange"></i> Meeting in 1 hour <span class="dropdown-time">1 hour</span></a></li>
								<li><a href="#"> <i class="fa fa-bell p-r-10 f-18"></i> Server 5 overloaded <span class="dropdown-time">2 hours</span></a></li>
								<li><a href="#"> <i	class="fa fa-comment p-r-10 f-18 c-gray"></i> Bill comment your post <span class="dropdown-time">3 hours</span></a></li>
								<li><a href="#"> <i	class="fa fa-picture-o p-r-10 f-18 c-blue"></i> New picture	added <span class="dropdown-time">2 days</span>	</a></li>
							</ul>
						</li>
						<li class="dropdown-footer clearfix"><a href="#" class="pull-left">Voir toutes les notifications</a> <a href="#" class="pull-right"> <i class="fa fa-cog"></i></a></li>
					</ul>
				</li>
				<li class="dropdown" id="user-header">
					<a href="#" class="dropdown-toggle c-white" data-toggle="dropdown" data-hover="dropdown" data-close-others="true"> 
						<img src="http://www.gravatar.com/avatar/${profile.email}?d=mm" alt="user avatar" width="30" class="p-r-5"> <span class="username">${profile.fullname}</span> <i class="fa fa-angle-down p-r-10"></i>
					</a>
					<ul class="dropdown-menu">
						<li>
							<a href="profile.sdo"> <i class="glyph-icon flaticon-account"></i> Mon Profile </a>
						</li>
						<li>
							<a href="tasks.sdo"> <i class="glyph-icon flaticon-calendar"></i> Mes tâches </a>
						</li>
						<li>
							<a href="settings.sdo"> <i	class="glyph-icon flaticon-settings21"></i> Paramètres </a>
						</li>
						<li class="dropdown-footer clearfix">
							<a href="javascript:;"	class="toggle_fullscreen" title="Fullscreen"> <i class="glyph-icon flaticon-fullscreen3"></i></a> 
							<a href="login.do?action=lock" title="Lock Screen"> <i class="glyph-icon flaticon-padlock23"></i></a> 
							<a href="login.do?action=logout" title="Logout"> <i	class="fa fa-power-off"></i></a>
						</li>
					</ul>
				</li>
			</ul>
		</div>
	</div>
</nav>