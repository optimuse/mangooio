<#include "header.ftl">
<section class="content-header">
	<h1>Dashboard</h1>
</section>
<section class="content">
<#if warnings?has_content>
<div class="row">
    <div class="col-lg-12">
		<div class="alert alert-warning alert-dismissible">
        		<button type="button" class="close" data-dismiss="alert" aria-hidden="true">×</button>
             <h4><i class="icon fa fa-warning"></i> Warning!</h4>
             <#list warnings as warning>
    				${warning}<br>
			</#list>
        </div>
    </div>
</div>
</#if>
<div class="row">
    <div class="col-lg-12">
    	<div class="info-box">
        	<span class="info-box-icon bg-green"><i class="fa fa-rocket"></i></span>
            <div class="info-box-content">
            	<span class="info-box-text">Application started</span>
            	<span class="info-box-number">${started}<br/>${prettytime(uptime)}</span>
            </div>
        </div>
    </div>
</div>
<div class="row">
    <div class="col-lg-6">
    	<div class="info-box">
        	<span class="info-box-icon bg-aqua"><i class="fa fa-line-chart"></i></span>
            <div class="info-box-content">
            	<span class="info-box-text">Free memory</span>
            	<span class="info-box-number">${freeMemory}</span>
            </div>
        </div>
    </div>
    <div class="col-lg-6">
    	<div class="info-box">
        	<span class="info-box-icon bg-aqua"><i class="fa fa-battery-4"></i></span>
            <div class="info-box-content">
            	<span class="info-box-text">Allocated memory</span>
            	<span class="info-box-number">${allocatedMemory}</span>
            </div>
        </div>
    </div>
</div>
</section>
<#include "footer.ftl">