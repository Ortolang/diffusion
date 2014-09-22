<nav id="sidebar">
	<div id="main-menu">
		<ul class="sidebar-nav">
			<li class="current"><a href="home.sdo"><i class="fa fa-dashboard"></i><span class="sidebar-text">Tableau de bord</span></a></li>
			<li><a href="notifications.sdo"><i class="glyph-icon flaticon-charts2"></i><span class="sidebar-text">Notifications</span><span class="pull-right badge badge-primary">6</span></a></li>
			<li><a href="tasks.sdo"><i class="glyph-icon flaticon-widgets"></i><span class="sidebar-text">Tâches<span class="label label-danger pull-right">New</span></span></a></li>
			<li><a href="#"><i class="glyph-icon flaticon-forms"></i><span class="sidebar-text">Serveur</span><span class="fa arrow"></span></a>
				<ul class="submenu collapse">
					<li><a href="serverstate.sdo"><span class="sidebar-text">Etat</span></a></li>
					<li><a href="serverconfig.sdo"><span class="sidebar-text">Configuration</span></a></li>
					<li><a href="serverlogs.sdo"><span class="sidebar-text">Logs</span></a></li>
				</ul>
			</li>
			<li><a href="#"><i class="glyph-icon flaticon-ui-elements2"></i><span class="sidebar-text">Entrepôt</span><span class="fa arrow"></span></a>
				<ul class="submenu collapse">
					<li><a href="repositorystats.sdo"><span class="sidebar-text">Statistiques</span></a></li>
					<li><a href="registry.sdo"><span class="sidebar-text">Registre</span></a></li>
					<li><a href="binarystore.sdo"><span class="sidebar-text">Base Binaire</span></a></li>
					<li><a href="indexstore.sdo"><span class="sidebar-text">Base d'indexes</span></a></li>
					<li><a href="semanticstore.sdo"><span class="sidebar-text">Base sémantique</span></a></li>
				</ul>
			</li>
			<li><a href="#"><i class="glyph-icon flaticon-pages"></i><span class="sidebar-text">Utilisateurs</span><span class="fa arrow"></span></a>
				<ul class="submenu collapse">
					<li><a href="useraccounts.sdo"><span class="sidebar-text">Comptes</span></a></li>
					<li><a href="quotas.sdo"><span class="sidebar-text">Quotas</span></a></li>
					<li><a href="messages.sdo"><span class="sidebar-text">Message</span></a></li>
				</ul>
			</li>
			<li><a href="#"><i class="glyph-icon flaticon-pages"></i><span class="sidebar-text">Espaces de travail</span><span class="fa arrow"></span></a>
				<ul class="submenu collapse">
					<li><a href="useraccounts.sdo"><span class="sidebar-text">Navigation</span></a></li>
					<li><a href="quotas.sdo"><span class="sidebar-text">Quotas</span></a></li>
				</ul>
			</li>
		</ul>
	</div>
	<div class="footer-widget">
		<img src="img/gradient.png" alt="gradient effet" class="sidebar-gradient-img" />
		<div id="sidebar-charts">
			<div class="sidebar-charts-inner">
				<div class="sidebar-charts-left">
					<div class="sidebar-chart-title">Requêtes</div>
					<div class="sidebar-chart-number">1,256,456</div>
				</div>
				<div class="sidebar-charts-right" class="sparkline mini-chart" data-type="bar" data-color="theme">
					<span class="dynamicbar1"></span>
				</div>
			</div>
			<hr class="divider">
			<div class="sidebar-charts-inner">
				<div class="sidebar-charts-left">
					<div class="sidebar-chart-title">Objets</div>
					<div class="sidebar-chart-number">472,564</div>
				</div>
				<div class="sidebar-charts-right" class="sparkline mini-chart" data-type="bar" data-color="theme">
					<span class="dynamicbar2"></span>
				</div>
			</div>
			<hr class="divider">
			<div class="sidebar-charts-inner">
				<div class="sidebar-charts-left">
					<div class="sidebar-chart-title">Utilisateurs</div>
					<div class="sidebar-chart-number" id="number-visits">3,687</div>
				</div>
				<div class="sidebar-charts-right" class="sparkline mini-chart" data-type="bar" data-color="theme">
					<span class="dynamicbar3"></span>
				</div>
			</div>
		</div>
		<div class="sidebar-footer clearfix">
			<a class="pull-left" href="profile.sdo" rel="tooltip" data-placement="top" data-original-title="Settings"><i class="glyph-icon flaticon-settings21"></i></a> 
			<a class="pull-left toggle_fullscreen" href="#" rel="tooltip" data-placement="top" data-original-title="Fullscreen"><i class="glyph-icon flaticon-fullscreen3"></i></a> 
			<a class="pull-left" href="login.do?action=lock" rel="tooltip" data-placement="top" data-original-title="Lockscreen"><i class="glyph-icon flaticon-padlock23"></i></a> 
			<a class="pull-left" href="login.do?action=logout" rel="tooltip" data-placement="top" data-original-title="Logout"><i class="fa fa-power-off"></i></a>
		</div>
	</div>
</nav>