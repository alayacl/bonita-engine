DELETE FROM user_membership WHERE tenantid = ${tenantid}
GO
DELETE FROM p_metadata_val WHERE tenantid = ${tenantid}
GO
DELETE FROM p_metadata_def WHERE tenantid = ${tenantid}
GO
DELETE FROM user_ WHERE tenantid = ${tenantid}
GO
DELETE FROM user_contactinfo WHERE tenantid = ${tenantid}
GO
DELETE FROM role WHERE tenantid = ${tenantid}
GO
DELETE FROM group_ WHERE tenantid = ${tenantid}
GO
