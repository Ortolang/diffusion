update 
  authorisationpolicy as ap
set
  rulescontent = E'#Fri Jan 13 16:17:52 CET 2017
anonymous=read
esr=read,download
$\{workspace.privileged\}=read,download
'
from
  authorisationpolicy, authorisationpolicytemplate as apt
where 
  apt.name = 'esr'
and
  ap.id = apt.template;