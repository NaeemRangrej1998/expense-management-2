'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Check,
  ChevronLeft,
  ChevronRight,
  KeyRound,
  MapPin,
  Pencil,
  Plus,
  Search,
  ShieldCheck,
  ToggleLeft,
  ToggleRight,
  Users,
  X,
} from 'lucide-react';
import { toast } from 'sonner';
import { AppShell } from '@/components/AppShell';
import { useAuth } from '@/hooks/useAuth';
import { apiClient, getApiError } from '@/lib/api-client';
import { AreaResponse, PageResponse, PermissionResponse, RoleResponse, UserResponse } from '@/types';

type AdminResource = 'users' | 'roles' | 'permissions' | 'areas';
type AdminRecord = UserResponse | RoleResponse | PermissionResponse | AreaResponse;

const pageConfig: Record<AdminResource, { title: string; subtitle: string; permission: string; icon: typeof Users }> = {
  users: { title: 'Users', subtitle: 'Manage your team and their workspace access.', permission: 'users.view', icon: Users },
  roles: { title: 'Roles', subtitle: 'Create roles and decide which actions they can perform.', permission: 'roles.view', icon: ShieldCheck },
  permissions: { title: 'Permissions', subtitle: 'A reference catalog of the actions available in this workspace.', permission: 'permissions.view', icon: KeyRound },
  areas: { title: 'Areas', subtitle: 'Organize your work across regions and local teams.', permission: 'areas.view', icon: MapPin },
};

const userStatusOptions = ['ACTIVE', 'INACTIVE'] as const;
const areaStatusOptions = ['ACTIVE', 'INACTIVE'] as const;

function formatStatus(status: string) {
  return status.charAt(0) + status.slice(1).toLowerCase();
}

function getErrorMessage(error: unknown) {
  return getApiError(error).message || 'The request could not be completed.';
}

export function AdminPage({ resource }: { resource: AdminResource }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isAuthenticated, isLoading: isAuthLoading, user } = useAuth();
  const config = pageConfig[resource];
  const Icon = config.icon;
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<AdminRecord | null>(null);
  const [formError, setFormError] = useState('');
  const [form, setForm] = useState({
    name: '',
    email: '',
    mobileNumber: '',
    password: '',
    description: '',
    roleId: '',
    areaIds: [] as number[],
    permissionIds: [] as number[],
    status: 'ACTIVE',
  });

  useEffect(() => {
    if (!isAuthLoading && !isAuthenticated) router.replace('/login');
  }, [isAuthLoading, isAuthenticated, router]);

  const canView = user?.permissions.includes(config.permission) ?? false;
  const canCreate = user?.permissions.includes(`${resource}.create`) ?? false;
  const canUpdate = user?.permissions.includes(`${resource}.update`) ?? false;
  const canToggleStatus = user?.permissions.includes(`${resource}.deactivate`) ?? false;
  const canViewPermissions = user?.permissions.includes('permissions.view') ?? false;
  const listQuery = useQuery({
    queryKey: ['admin', resource, search, statusFilter, page],
    enabled: isAuthenticated && canView,
    queryFn: async () => {
      if (resource === 'permissions') return (await apiClient.get<PermissionResponse[]>('/permissions')).data;
      const response = await apiClient.get<PageResponse<AdminRecord>>(`/${resource}`, {
        params: {
          ...(search && { search }),
          ...(resource !== 'roles' && statusFilter && { status: statusFilter }),
          page,
          size: 10,
        },
      });
      return response.data;
    },
  });
  const rolesQuery = useQuery({
    queryKey: ['admin-options', 'roles'],
    enabled: isAuthenticated && resource === 'users' && dialogOpen,
    queryFn: async () => (await apiClient.get<RoleResponse[]>('/roles/all')).data,
  });
  const areasQuery = useQuery({
    queryKey: ['admin-options', 'areas'],
    enabled: isAuthenticated && resource === 'users' && dialogOpen,
    queryFn: async () => (await apiClient.get<AreaResponse[]>('/areas/active')).data,
  });
  const permissionsQuery = useQuery({
    queryKey: ['admin-options', 'permissions'],
    enabled: isAuthenticated && resource === 'roles' && dialogOpen && canViewPermissions,
    queryFn: async () => (await apiClient.get<PermissionResponse[]>('/permissions')).data,
  });

  const pagedData = listQuery.data && !Array.isArray(listQuery.data) ? listQuery.data : undefined;
  const records = useMemo<AdminRecord[]>(() => {
    if (!listQuery.data) return [];
    if (resource === 'permissions') {
      const query = search.trim().toLowerCase();
      return (listQuery.data as PermissionResponse[]).filter((permission) =>
        !query || `${permission.name} ${permission.module} ${permission.action} ${permission.description ?? ''}`.toLowerCase().includes(query),
      );
    }
    if ('content' in listQuery.data) return listQuery.data.content;
    return listQuery.data;
  }, [listQuery.data, resource, search]);

  const totalElements = resource === 'permissions'
    ? records.length
    : pagedData?.totalElements ?? records.length;
  const totalPages = resource === 'permissions'
    ? 1
    : pagedData?.totalPages ?? 1;

  const saveMutation = useMutation({
    mutationFn: async (payload: Record<string, unknown>) => {
      const endpoint = resource === 'permissions' ? '/permissions' : `/${resource}`;
      if (editing) return apiClient.put(`${endpoint}/${editing.id}`, payload);
      return apiClient.post(endpoint, payload);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', resource] });
      await queryClient.invalidateQueries({ queryKey: ['admin-options'] });
      toast.success(`${config.title.slice(0, -1) || config.title} ${editing ? 'updated' : 'created'}.`);
      setDialogOpen(false);
      setEditing(null);
      setFormError('');
    },
    onError: (error) => setFormError(getErrorMessage(error)),
  });

  const statusMutation = useMutation({
    mutationFn: async ({ record, status }: { record: AdminRecord; status: string }) =>
      apiClient.patch(`/${resource}/${record.id}/status`, { status }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', resource] });
      toast.success(`${config.title.slice(0, -1)} status updated.`);
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  });

  function openCreate() {
    setEditing(null);
    setForm({ name: '', email: '', mobileNumber: '', password: '', description: '', roleId: '', areaIds: [], permissionIds: [], status: 'ACTIVE' });
    setFormError('');
    setDialogOpen(true);
  }

  function openEdit(record: AdminRecord) {
    setEditing(record);
    if (resource === 'users') {
      const item = record as UserResponse;
      setForm({
        name: item.name,
        email: item.email,
        mobileNumber: item.mobileNumber ?? '',
        password: '',
        description: '',
        roleId: String(item.roles[0]?.id ?? ''),
        areaIds: item.areas.map((area) => area.id),
        permissionIds: [],
        status: item.status,
      });
    } else if (resource === 'roles') {
      const item = record as RoleResponse;
      setForm({
        name: item.name,
        email: '',
        mobileNumber: '',
        password: '',
        description: item.description ?? '',
        roleId: '',
        areaIds: [],
        permissionIds: item.permissions.map((permission) => permission.id),
        status: 'ACTIVE',
      });
    } else if (resource === 'areas') {
      const item = record as AreaResponse;
      setForm({ name: item.name, email: '', mobileNumber: '', password: '', description: item.description ?? '', roleId: '', areaIds: [], permissionIds: [], status: item.status });
    }
    setFormError('');
    setDialogOpen(true);
  }

  function submitForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError('');
    if (resource === 'users') {
      if (!form.roleId) {
        setFormError('Choose a role for this user.');
        return;
      }
      if (!editing && form.password.length < 8) {
        setFormError('Password must be at least 8 characters.');
        return;
      }
      saveMutation.mutate({
        name: form.name.trim(),
        email: form.email.trim(),
        mobileNumber: form.mobileNumber.trim() || null,
        ...(!editing && { password: form.password }),
        roleId: Number(form.roleId),
        areaIds: form.areaIds,
        status: form.status,
      });
    } else if (resource === 'roles') {
      saveMutation.mutate({
        name: form.name.trim(),
        description: form.description.trim(),
        ...(canViewPermissions && { permissionIds: form.permissionIds }),
      });
    } else if (resource === 'areas') {
      saveMutation.mutate({
        name: form.name.trim(),
        description: form.description.trim(),
        status: form.status,
      });
    }
  }

  function toggleId(key: 'areaIds' | 'permissionIds', id: number) {
    setForm((current) => ({
      ...current,
      [key]: current[key].includes(id) ? current[key].filter((value) => value !== id) : [...current[key], id],
    }));
  }

  if (isAuthLoading || !isAuthenticated) return <main className="page-loading"><span className="loading-mark">G</span><span>Loading your workspace…</span></main>;

  const stateLabel = resource === 'users' ? 'Users' : resource === 'areas' ? 'Areas' : resource === 'roles' ? 'Roles' : 'Permissions';
  const canAdd = resource !== 'permissions' && canCreate;

  return (
    <AppShell title={config.title}>
      <div className="admin-page">
        <div className="page-heading-row">
          <div><div className="eyebrow"><span className="eyebrow-dot" /> WORKSPACE ADMINISTRATION</div><h1>{config.title}</h1><p>{config.subtitle}</p></div>
          {canAdd && <button className="button button-primary" onClick={openCreate}><Plus size={16} /> Add {resource === 'users' ? 'user' : resource === 'roles' ? 'role' : 'area'}</button>}
        </div>

        {!canView ? (
          <section className="panel admin-empty"><div className="admin-empty-icon"><ShieldCheck size={19} /></div><h2>You don&apos;t have access to {config.title.toLowerCase()}</h2><p>Ask an organization administrator to grant the <code>{config.permission}</code> permission.</p></section>
        ) : (
          <>
            <section className="admin-summary-strip">
              <span className="admin-summary-icon"><Icon size={17} /></span>
              <div><small>{resource === 'permissions' ? 'AVAILABLE PERMISSIONS' : `TOTAL ${stateLabel.toUpperCase()}`}</small><strong>{listQuery.isLoading ? '—' : totalElements}</strong></div>
              <span className="admin-summary-caption">{resource === 'permissions' ? 'Permission catalog' : 'Across your organization'}</span>
            </section>
            <section className="panel admin-table-panel">
              <div className="admin-table-toolbar">
                <form className="admin-search" onSubmit={(event) => event.preventDefault()}><Search size={16} /><input placeholder={`Search ${config.title.toLowerCase()}…`} aria-label={`Search ${config.title}`} value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} /></form>
                <div className="admin-table-filters">
                  {resource !== 'roles' && resource !== 'permissions' && <select className="admin-filter-select" aria-label="Filter by status" value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(0); }}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select>}
                  <span className="admin-count-label">{listQuery.isLoading ? 'Loading…' : `${records.length} shown`}</span>
                </div>
              </div>
              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead><tr>{resource === 'users' ? <><th>USER</th><th>ROLE</th><th>AREAS</th><th>STATUS</th><th className="admin-action-cell">ACTIONS</th></> : resource === 'roles' ? <><th>ROLE</th><th>DESCRIPTION</th><th>PERMISSIONS</th><th>TYPE</th><th className="admin-action-cell">ACTIONS</th></> : resource === 'areas' ? <><th>AREA</th><th>DESCRIPTION</th><th>STATUS</th><th>CREATED</th><th className="admin-action-cell">ACTIONS</th></> : <><th>PERMISSION</th><th>MODULE</th><th>ACTION</th><th>DESCRIPTION</th></>}</tr></thead>
                  <tbody>
                    {records.map((record) => resource === 'users' ? renderUserRow(record as UserResponse, canUpdate, canToggleStatus, openEdit, (status) => statusMutation.mutate({ record, status }), statusMutation.isPending)
                      : resource === 'roles' ? renderRoleRow(record as RoleResponse, canUpdate, openEdit)
                        : resource === 'areas' ? renderAreaRow(record as AreaResponse, canUpdate, canToggleStatus, openEdit, (status) => statusMutation.mutate({ record, status }), statusMutation.isPending)
                          : renderPermissionRow(record as PermissionResponse))}
                    {listQuery.isLoading && <tr><td className="admin-table-message" colSpan={resource === 'permissions' ? 4 : 5}>Loading {config.title.toLowerCase()}…</td></tr>}
                    {listQuery.isError && <tr><td className="admin-table-message admin-table-error" colSpan={resource === 'permissions' ? 4 : 5}>{getErrorMessage(listQuery.error)} <button onClick={() => void listQuery.refetch()}>Try again</button></td></tr>}
                    {!listQuery.isLoading && !listQuery.isError && records.length === 0 && <tr><td className="admin-table-message" colSpan={resource === 'permissions' ? 4 : 5}>{search ? `No ${config.title.toLowerCase()} match “${search}”.` : `No ${config.title.toLowerCase()} found.`}</td></tr>}
                  </tbody>
                </table>
              </div>
              {resource !== 'permissions' && <div className="admin-pagination"><span>{records.length ? `Showing ${page * 10 + 1}–${page * 10 + records.length} of ${totalElements}` : `0 of ${totalElements}`}</span><div><button className="pagination-button" disabled={page === 0 || listQuery.isLoading} onClick={() => setPage((value) => Math.max(0, value - 1))} aria-label="Previous page"><ChevronLeft size={16} /></button><span>Page {page + 1} of {Math.max(totalPages, 1)}</span><button className="pagination-button" disabled={page + 1 >= totalPages || listQuery.isLoading} onClick={() => setPage((value) => value + 1)} aria-label="Next page"><ChevronRight size={16} /></button></div></div>}
            </section>
            {resource === 'permissions' && <div className="admin-readonly-note"><KeyRound size={15} /> Permissions are defined by the backend and can be assigned to roles from the Roles page.</div>}
          </>
        )}
      </div>

      {dialogOpen && resource !== 'permissions' && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDialogOpen(false); }}>
        <section className="transaction-modal admin-modal" role="dialog" aria-modal="true" aria-labelledby="admin-dialog-title">
          <div className="modal-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> {editing ? 'UPDATE WORKSPACE' : 'NEW WORKSPACE RECORD'}</div><h2 id="admin-dialog-title">{editing ? `Edit ${resource === 'users' ? 'user' : resource === 'roles' ? 'role' : 'area'}` : `Add ${resource === 'users' ? 'user' : resource === 'roles' ? 'role' : 'area'}`}</h2><p>{resource === 'users' ? 'Set the account details, role, and area access.' : resource === 'roles' ? 'Choose a name and assign the permissions this role needs.' : 'Give this area a clear name your team will recognize.'}</p></div><button className="icon-button" onClick={() => setDialogOpen(false)} aria-label="Close dialog"><X size={19} /></button></div>
          <form className="transaction-form admin-form" onSubmit={submitForm}>
            {resource === 'users' && <>
              <label className="form-field form-field-wide"><span>Full name <b>*</b></span><input maxLength={200} required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="e.g. Asha Patel" /></label>
              <label className="form-field form-field-wide"><span>Email address <b>*</b></span><input type="email" maxLength={255} required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="name@organization.org" /></label>
              <label className="form-field form-field-wide"><span>Mobile number <small>Optional</small></span><input maxLength={20} value={form.mobileNumber} onChange={(event) => setForm({ ...form, mobileNumber: event.target.value })} placeholder="Phone number" /></label>
              {!editing && <label className="form-field form-field-wide"><span>Initial password <b>*</b></span><input type="password" autoComplete="new-password" minLength={8} required value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="At least 8 characters" /><small className="field-hint">The user can change it later using “Forgot password?” on the sign-in page.</small></label>}
              <label className="form-field form-field-wide"><span>Role <b>*</b></span><select required value={form.roleId} onChange={(event) => setForm({ ...form, roleId: event.target.value })}><option value="">{rolesQuery.isLoading ? 'Loading roles…' : 'Choose a role'}</option>{rolesQuery.data?.map((role) => <option key={role.id} value={role.id} disabled={role.name === 'Super Admin' && !user?.roles.includes('Super Admin')}>{role.name}</option>)}</select>{rolesQuery.isError && <small className="field-hint field-error">Could not load roles: {getErrorMessage(rolesQuery.error)}</small>}</label>
              <div className="form-field form-field-wide"><span>Area access <small>Optional</small></span>{areasQuery.isLoading ? <small className="field-hint">Loading areas…</small> : areasQuery.isError ? <small className="field-hint field-error">Could not load areas: {getErrorMessage(areasQuery.error)}</small> : <div className="admin-check-grid">{areasQuery.data?.map((area) => <label className="admin-check-option" key={area.id}><input type="checkbox" checked={form.areaIds.includes(area.id)} onChange={() => toggleId('areaIds', area.id)} /><span>{area.name}</span></label>)}</div>}</div>
              <label className="form-field form-field-wide"><span>Account status</span><select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>{userStatusOptions.map((status) => <option key={status} value={status}>{formatStatus(status)}</option>)}</select></label>
            </>}
            {resource === 'roles' && <>
              <label className="form-field form-field-wide"><span>Role name <b>*</b></span><input maxLength={100} required disabled={Boolean((editing as RoleResponse | null)?.systemRole)} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="e.g. Area Coordinator" /></label>
              {(editing as RoleResponse | null)?.systemRole && <small className="field-hint form-field-wide">This is a system role. Its name cannot be changed.</small>}
              <label className="form-field form-field-wide"><span>Description <small>Optional</small></span><textarea rows={2} maxLength={500} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="Describe this role’s responsibilities." /></label>
              <div className="form-field form-field-wide"><span>Permissions {canViewPermissions && `(${form.permissionIds.length} selected)`}</span>{!canViewPermissions ? <small className="field-hint">You need the permissions.view permission to change role permissions. Existing assignments will be preserved.</small> : permissionsQuery.isLoading ? <small className="field-hint">Loading permissions…</small> : permissionsQuery.isError ? <small className="field-hint field-error">Could not load permissions: {getErrorMessage(permissionsQuery.error)}</small> : <PermissionSelector permissions={permissionsQuery.data ?? []} selected={form.permissionIds} onToggle={(id) => toggleId('permissionIds', id)} />}</div>
            </>}
            {resource === 'areas' && <>
              <label className="form-field form-field-wide"><span>Area name <b>*</b></span><input maxLength={200} required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="e.g. North Zone" /></label>
              <label className="form-field form-field-wide"><span>Description <small>Optional</small></span><textarea rows={3} maxLength={500} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="A short description of this area." /></label>
              <label className="form-field form-field-wide"><span>Status</span><select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>{areaStatusOptions.map((status) => <option key={status} value={status}>{formatStatus(status)}</option>)}</select></label>
            </>}
            {formError && <div className="login-error form-error" role="alert">{formError}</div>}
            <div className="modal-actions"><button type="button" className="button button-outline" onClick={() => setDialogOpen(false)}>Cancel</button><button type="submit" className="button button-primary" disabled={saveMutation.isPending || (resource === 'users' && (rolesQuery.isLoading || areasQuery.isLoading)) || (resource === 'roles' && canViewPermissions && permissionsQuery.isLoading)}>{saveMutation.isPending ? 'Saving…' : editing ? 'Save changes' : `Create ${resource === 'users' ? 'user' : resource === 'roles' ? 'role' : 'area'}`}<Check size={15} /></button></div>
          </form>
        </section>
      </div>}
    </AppShell>
  );
}

function PermissionSelector({ permissions, selected, onToggle }: { permissions: PermissionResponse[]; selected: number[]; onToggle: (id: number) => void }) {
  const grouped = useMemo(() => permissions.reduce<Record<string, PermissionResponse[]>>((groups, permission) => {
    (groups[permission.module] ??= []).push(permission);
    return groups;
  }, {}), [permissions]);

  return <div className="permission-selector">{Object.entries(grouped).map(([module, items]) => <section className="permission-module" key={module}><h3>{module}</h3>{items.map((permission) => <label className="admin-check-option permission-option" key={permission.id}><input type="checkbox" checked={selected.includes(permission.id)} onChange={() => onToggle(permission.id)} /><span><strong>{permission.action}</strong><small>{permission.description || permission.name}</small></span></label>)}</section>)}</div>;
}

function renderUserRow(user: UserResponse, canUpdate: boolean, canToggle: boolean, onEdit: (record: AdminRecord) => void, onToggle: (status: string) => void, pending: boolean) {
  return <tr key={user.id}><td><span className="admin-identity"><span className="admin-avatar">{user.name.slice(0, 1).toUpperCase()}</span><span><strong>{user.name}</strong><small>{user.email}</small></span></span></td><td><span className="admin-role-chip"><ShieldCheck size={12} />{user.roles[0]?.name ?? 'No role'}</span></td><td><span className="admin-area-list">{user.areas.length ? user.areas.map((area) => area.name).join(', ') : 'All areas'}</span></td><td><StatusBadge status={user.status} /></td><td className="admin-action-cell"><RowActions canUpdate={canUpdate} canToggle={canToggle} active={user.status === 'ACTIVE'} pending={pending} onEdit={() => onEdit(user)} onToggle={() => onToggle(user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')} /></td></tr>;
}

function renderRoleRow(role: RoleResponse, canUpdate: boolean, onEdit: (record: AdminRecord) => void) {
  return <tr key={role.id}><td><span className="admin-identity"><span className="admin-avatar role-avatar"><ShieldCheck size={15} /></span><span><strong>{role.name}</strong><small>{role.systemRole ? 'Built-in system role' : 'Custom role'}</small></span></span></td><td><span className="admin-description">{role.description || '—'}</span></td><td><span className="admin-count-chip"><KeyRound size={12} />{role.permissions.length} permissions</span></td><td><span className={`admin-type-pill ${role.systemRole ? 'system-type' : ''}`}>{role.systemRole ? 'System' : 'Custom'}</span></td><td className="admin-action-cell">{canUpdate && <button className="admin-row-action" onClick={() => onEdit(role)} aria-label={`Edit ${role.name}`}><Pencil size={14} /> Edit</button>}</td></tr>;
}

function renderAreaRow(area: AreaResponse, canUpdate: boolean, canToggle: boolean, onEdit: (record: AdminRecord) => void, onToggle: (status: string) => void, pending: boolean) {
  return <tr key={area.id}><td><span className="admin-identity"><span className="admin-avatar area-avatar"><MapPin size={15} /></span><span><strong>{area.name}</strong><small>Area #{area.id}</small></span></span></td><td><span className="admin-description">{area.description || '—'}</span></td><td><StatusBadge status={area.status} /></td><td>{new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(new Date(area.createdAt))}</td><td className="admin-action-cell"><RowActions canUpdate={canUpdate} canToggle={canToggle} active={area.status === 'ACTIVE'} pending={pending} onEdit={() => onEdit(area)} onToggle={() => onToggle(area.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')} /></td></tr>;
}

function renderPermissionRow(permission: PermissionResponse) {
  return <tr key={permission.id}><td><span className="admin-permission-name"><KeyRound size={14} /><strong>{permission.name}</strong></span></td><td><span className="admin-module-chip">{permission.module}</span></td><td><span className="admin-action-chip">{permission.action}</span></td><td><span className="admin-description">{permission.description || '—'}</span></td></tr>;
}

function StatusBadge({ status }: { status: string }) {
  return <span className={`status-pill status-${status.toLowerCase()}`}><i />{formatStatus(status)}</span>;
}

function RowActions({ canUpdate, canToggle, active, pending, onEdit, onToggle }: { canUpdate: boolean; canToggle: boolean; active: boolean; pending: boolean; onEdit: () => void; onToggle: () => void }) {
  return <span className="admin-row-actions">{canUpdate && <button className="admin-row-action" onClick={onEdit}><Pencil size={14} /> Edit</button>}{canToggle && <button className={`admin-icon-action ${active ? 'deactivate-action' : 'activate-action'}`} disabled={pending} onClick={onToggle} aria-label={active ? 'Deactivate' : 'Activate'} title={active ? 'Deactivate' : 'Activate'}>{active ? <ToggleRight size={19} /> : <ToggleLeft size={19} />}</button>}</span>;
}
